import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*questa classe gestisce il calcolo assegnazione e invio delle notifiche dei reward , apre una 
connessione multicast in modo che i client possano collegarsi ed ascoltare le notifiche */

public class Rewards implements Runnable {
    private final ConfigReader configReader;
    private final SocialManager socialManager;

    private InetAddress multicaAddress;
    private int multicastPort;

    private String lastRewardCheck;
    private Date date;

    private static final int RewardAuthor = 70;
    private static final int RewardCurator = 30;
    private static final String reasonAuthor = "Ricompensa per il post: ";
    private static final String reasonCurator = "Ricompensa per aver fatto il curatore di :";

    private volatile boolean stop = false;

    public Rewards(ConfigReader c, SocialManager s) {
        this.configReader = c;
        this.socialManager = s;
        try {
            this.multicaAddress = InetAddress.getByName(configReader.getConfigValue("MulticastAddress"));
        } catch (UnknownHostException ignored) {
        }
        this.multicastPort = Integer.parseInt(configReader.getConfigValue("MulticastPort"));
        this.lastRewardCheck = configReader.getConfigValue("LastRewardCheck");
    }

    // metodo che blocca il thread si assegnazione premi
    public void stopServer() {
        this.stop = true;
    }

    // metodo che salva la data dell'ultimo Reward sul file di config
    public void saveLastReward() {
        configReader.changeSaveConfig("LastRewardCheck", lastRewardCheck);
    }

    @Override
    public void run() {

        try (DatagramSocket serveDatagramSocket = new DatagramSocket(null)) {
            // poteva essere fatto anche con getLocalHost()
            InetAddress inetAddress = InetAddress.getByName("localhost");
            InetSocketAddress serveSocketAddress = new InetSocketAddress(inetAddress, this.multicastPort);
            serveDatagramSocket.setReuseAddress(true);// consente di collegare il socket anche se una precedente
                                                      // connessione è in stato di timeout
            serveDatagramSocket.bind(serveSocketAddress);

            byte[] byteArray;

            ConcurrentHashMap<Integer, Post> posts;
            // while(stopServer())
            while (!stop) {
                this.date = new Date();
                posts = socialManager.getPostList();
                if (posts.size() > 0) {
                    double reward = 0;
                    double totalReward = 0;
                    for (Post p : posts.values()) {
                        reward = gainFormula(p, date);
                        totalReward = totalReward + reward;
                    }
                    if (totalReward > 0) {
                        byteArray = socialManager.formattedWincoin(totalReward).getBytes();
                        // invio la lunghezza della stringa che il client riceverà
                        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).putInt(byteArray.length);
                        DatagramPacket dPacket = new DatagramPacket(buffer.array(), buffer.limit(), this.multicaAddress,
                                this.multicastPort);
                        serveDatagramSocket.send(dPacket);
                        // invio della stringa vera e propria
                        dPacket = new DatagramPacket(byteArray, byteArray.length, this.multicaAddress,
                                this.multicastPort);
                        serveDatagramSocket.send(dPacket);
                        System.out.println("Notifica del guadagno totale di " + totalReward + " inviata.");
                    }

                    this.lastRewardCheck = date.toString();
                }
                // metto a dormire il thread del reward manager per fargloi fare un altro
                // controllo dopo tot tempo
                try {
                    Thread.sleep(Integer.parseInt(this.configReader.getConfigValue("RewardCheckSleep")));
                } catch (InterruptedException e) {
                    DEBUG.messaggioDiDebug("Interruptedexception");
                }
            }
        } catch (IOException e) {
            DEBUG.messaggioDiDebug("IOexception");
        }

    }

    // metodo che si occupa di calcolare il gain secondo al formula riportat sulle
    // specifiche
    public double gainFormula(Post p, Date d) {
        double reward;
        p.addIteration();
        int numIt = p.getIteration();// iterazione del post
        // inizio primo logaritmo
        double log = 0;
        ConcurrentHashMap<String, Vote> voteList = p.newPeopleLike(d);
        for (Vote v : voteList.values()) {
            log += v.getVote();
        }
        if (log < 0) {// l'argomento del logaritmo non può essere negativo
            log = 0;
            // max tra sommatoria e zero =0
        }
        log = log + 1;
        log = Math.log(log);
        // prima sommatoria fatta

        // inizio secondo logaritmo
        double log2 = 0;
        HashSet<String> commentingUser = (HashSet<String>) p.getListUserCommentingAfterDate(d);
        for (String u : commentingUser) {
            int totalComment = p.getCommentByUser(u).size();
            log2 = log2 + (2 / (1 + Math.pow(Math.E, -(totalComment - 1))));
        }
        log2++;
        log2 = Math.log(log2);
        // fine secondo logaritmo

        double logSum = log2 + log;
        reward = logSum / numIt;

        if (reward != 0) {
            // aggiorno i wallet di autore e curatori
            Set<String> curators = new LinkedHashSet<>();
            curators.addAll(voteList.keySet());
            curators.addAll(commentingUser);
            if (curators.size() == 0) {
                return reward;
            }
            double curatorReward = (reward * (RewardCurator / 100)) / curators.size();
            for (String c : curators) {
                // aggiorno i portafogli dei curatori
                socialManager.getWallet(c).amountUpdate(reasonCurator + p.getpostId(), curatorReward);
            }
            // aggiorno il wallet dell'autore:
            socialManager.getWallet(p.getOwner()).amountUpdate(reasonAuthor + p.getpostId(),
                    (reward * (RewardAuthor / 100)));

        }
        return reward;
    }

}
