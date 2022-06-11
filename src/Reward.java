import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Date;

/*questa classe gestisce il calcolo assegnazione e invio delle notifiche dei reward , apre una 
connessione multicast in modo che i client possano collegarsi ed ascoltare le notifiche */

public class Reward implements Runnable {
    private final ConfigReader configReader;
    private final SocialManager socialManager;

    private InetAddress multicaAddress;
    private int multicastPort;

    private String lastRewardCheck;

    private static final int RewardAuthor = 70;
    private static final int RewardCurator = 30;
    private static final String reasonAuthor = "Ricompensa per il post: ";
    private static final String reasonCurator = "Ricompensa per aver fatto il curatore di :";

    private volatile boolean stop = false;

    public Reward(ConfigReader c, SocialManager s) {
        this.configReader = c;
        this.socialManager = s;
        try {
            this.multicaAddress = InetAddress.getByName(configReader.getConfigValue("MulticastAddress"));
        } catch (UnknownHostException ignored) {
        }
        this.multicastPort = Integer.parseInt(configReader.getConfigValue("MulticastPort"));
        this.lastRewardCheck = configReader.getConfigValue("LastRewardCheck");
    }

    // metodo che salva la data dell'ultimo Reward sul file di config
    public void saveLastReward() {
        configReader.changeSaveConfig("LastRewardCheck", lastRewardCheck);
    }

    public void stopExecution() {
        this.stop = true;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

}
