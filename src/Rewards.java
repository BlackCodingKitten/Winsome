import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import Color.ColoredText;

/*questa classe gestisce il calcolo assegnazione e invio delle notifiche dei reward , apre una 
connessione multicast in modo che i client possano collegarsi ed ascoltare le notifiche */

public class Rewards implements Runnable {
    private final ConfigReader configReader;
    private final SocialManager socialManager;

    private InetAddress multicaAddress;
    private int multicastPort;
    private int datagramSocketPort;

    private String lastRewardCheck;

    private static final double RewardAuthor = 70 / 100; // -> 70%
    private static final double RewardCurator = 30 / 100;// ->30%
    private static final String reasonAuthor = "Ricompensa per il post: ";
    private static final String reasonCurator = "Ricompensa per aver fatto il curatore del post :";

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
        this.datagramSocketPort = Integer.parseInt(configReader.getConfigValue("DatagramSocketPort"));
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

        try (DatagramSocket serveDatagramSocket = new DatagramSocket(datagramSocketPort)) {
            DatagramPacket datagramPacket;
            ConcurrentHashMap<Integer, Post> posts;

            // while(stopServer())
            while (!stop) {
                posts = socialManager.getPostList();
                double total = 0;
                Date date = new Date();

                try {
                    Thread.sleep(Integer.parseInt(configReader.getConfigValue("RewardCheckSleep")));

                } catch (InterruptedException ignore) {
                    // ignored
                }

                if (posts.size() != 0) {
                    for (Post post : posts.values()) {
                        total += gainFormula(post, date);
                    }

                }
                // approssimo a 4 cifre decimali
                total = SharedMethods.approximateDouble(total);
                if (total > 0) {
                    String toSend = ColoredText.ANSI_PURPLE + String.valueOf(total) + ColoredText.ANSI_RESET;
                    // invio la lunghezza della stringa contenente il reward
                    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);

                    buffer.putInt(toSend.getBytes().length);

                    datagramPacket = new DatagramPacket(buffer.array(), buffer.limit(), multicaAddress, multicastPort);

                    serveDatagramSocket.send(datagramPacket);

                    // dopo la dimensione invio la stringa contente il reward
                    buffer.clear();

                    buffer = ByteBuffer.allocate(toSend.getBytes().length);

                    buffer.put(toSend.getBytes());

                    datagramPacket = new DatagramPacket(buffer.array(), buffer.limit(), multicaAddress, multicastPort);

                    serveDatagramSocket.send(datagramPacket);

                }

                this.lastRewardCheck = date.toString();
                saveLastReward();
            }
        } catch (IOException e) {
            DEBUG.messaggioDiDebug("IOexception");
            e.printStackTrace();
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
            double curatorReward = reward / curators.size();
            for (String c : curators) {
                // aggiorno i portafogli dei curatori:
                socialManager.getWallet(c).amountUpdate(reasonCurator + p.getpostId(),
                        SharedMethods.approximateDouble(curatorReward * RewardCurator));
            }
            // aggiorno il wallet dell'autore:
            socialManager.getWallet(p.getOwner()).amountUpdate(reasonAuthor + p.getpostId(),
                    SharedMethods.approximateDouble(reward * RewardAuthor));

        }
        return reward;
    }

}
