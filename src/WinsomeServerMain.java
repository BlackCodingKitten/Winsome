import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
/*questa è la classe Main lato server, che esegue le sue operazioni con l'aiuto del connection handler. 
 * ogni connection handler gestisce una singola connessione.
 * 
 */

public class WinsomeServerMain {
    public static ConfigReader configReader;
    public static JsonFileManager fileManager;
    public static Rewards rewards;
    public static Backup dataBackup;
    public static ServerSocket serverSocket;
    public static final SocialManager socialManager;
    public static final ConcurrentLinkedQueue<Socket> socketlist;

    public static final ConcurrentHashMap<String, ClientSession> clientSessionList = new ConcurrentHashMap<>();
    public static final ConcurrentLinkedQueue<Socket> clientSocketList = new ConcurrentLinkedQueue<>();

    private int serverPort;

    private String 
    //attributi letti dal file di config
    public static void main(String[] args) {
        
    }
}
