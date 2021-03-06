import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
/*questa è la classe Main lato server, che esegue le sue operazioni con l'aiuto del connection handler. 
 * ogni connection handler gestisce una singola connessione.
 * 
 */
import java.util.concurrent.ThreadPoolExecutor;

public class WinsomeServerMain {
    public static ConfigReader configReader;
    public static JsonFileManager fileManager;
    public static Rewards rewards;
    public static Backup dataBackup;
    public static ServerSocket serverSocket;
    public static SocialManager socialManager;

    public static final ConcurrentHashMap<String, ClientSession> clientSessionList = new ConcurrentHashMap<>();
    public static final ConcurrentLinkedQueue<Socket> clientSocketList = new ConcurrentLinkedQueue<>();

    // valori da prendeere nel file di config
    private static int serverPort;
    private static String RmiServerRegistry;
    private static int RmiServerPort;
    private static int callbackClientPort;
    // private static int shutdown;
    private static String RmiCallbackClientRegistry;
    private static RmiCallback winsomeCallback;
    private static RmiService winsomeService;

    public static void main(String[] args) {
        System.out.println("Server Winsome in fase di attivazione...\n");
        try {
            configReader = new ConfigReader(true);
            // con la flag true dico al config file che è un server
        } catch (IOException e) {
            System.err.println("Impossibile avviare il server, errore nei file di configuarzione.");
            return;
        }

        serverPort = Integer.parseInt(configReader.getConfigValue("ServerPort"));
        RmiServerPort = Integer.parseInt(configReader.getConfigValue("RmiServerPort"));
        callbackClientPort = Integer.parseInt(configReader.getConfigValue("RmiClientCallbackPort"));
        RmiCallbackClientRegistry = configReader.getConfigValue("RmiCallbackClientRegistryName");
        RmiServerRegistry = configReader.getConfigValue("ServerRmiRegistryName");

        System.out.println(
                "Lettura file di configurazione terminata, impostazioni settate, WinsomeServer pronto all'avvio.");
        // dopo aver recuperato le informazioni dal file di config avvio il server

        System.out.println("Inizializzazione funzionalità di Winsome, Attendere prego");
        socialManager = new SocialManager(configReader);
        rewards = new Rewards(configReader, socialManager);
        // avvio il thread del reward manager
        System.out.println("Avvio gestore ricompense.");
        Thread rewardManager = new Thread(rewards);
        rewardManager.start();
        // leggo i dati dai json
        System.out.println("Caricamento Database Winsome...\nLettura file in corso...");
        fileManager = new JsonFileManager();
        fileManager.loadBackupFile(socialManager);
        System.out.println("Caricamento dati completato.");
        // utilizzo un threadpool di tipo cached perchè genera i thread solo quando
        // servono eli riusa appena possibile
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        // creazione del thread gestore del backup
        dataBackup = new Backup(configReader, fileManager, socialManager, rewards);
        Thread dataBackupThread = new Thread(dataBackup);

        // RmiService (register)
        winsomeService = new RmiService();
        try {
            RmiServiceInterface stub = (RmiServiceInterface) UnicastRemoteObject.exportObject(winsomeService, 0);
            LocateRegistry.createRegistry(RmiServerPort);
            Registry registry = LocateRegistry.getRegistry(RmiServerPort);
            registry.bind(RmiServerRegistry, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
            return;
        }
        // RmiCallback
        winsomeCallback = new RmiCallback();
        try {
            RmiCallbackInterface stub = (RmiCallbackInterface) UnicastRemoteObject.exportObject(winsomeCallback, 0);
            LocateRegistry.createRegistry(callbackClientPort);
            Registry registry = LocateRegistry.getRegistry(callbackClientPort);
            registry.bind(RmiCallbackClientRegistry, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            // avvio la socket del server
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server in ascolto sulla porta " + serverPort + ".\n");
        } catch (IOException e) {
            return;
        }

        // avvio il thread del backup
        dataBackupThread.start();

        while (true) {
            try {
                // accetta la connessione del client
                Socket client = serverSocket.accept();
                // aggiunge la connessione alla lista
                clientSocketList.add(client);
                // assegna il client ad un connectionhandler che gestisce tutto
                pool.submit(new ConnectionHandler(client, configReader, socialManager));
            } catch (SocketException e) {
                // si verifica quando si chiude forzatamente il server
                // quando l'admin digita stopserver
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
