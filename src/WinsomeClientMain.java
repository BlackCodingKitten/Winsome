import java.io.IOException;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Scanner;

/*classe client che contine il metodo main, 
gestisce tutte le operazioni che è in grado di svolgere il client, 
che essenzialmente sono inviare richieste e attendere risposte dal server,
 le richieste principali che il client fa al server sono la register , login e logout
  per le quali vine controllata la risposta del server  */

public class WinsomeClientMain {
    public static final boolean CLIENT = false;
    private static final DEBUG debug = new DEBUG();

    public static String serverAddress;
    public static int serverPort;
    public static int serverRmiPort;
    public static int clientRmiPort;
    public static String serverRmiRegistryName;
    public static String serverRmiCallbackRegistryName;
    public static int multicastPort;
    public static String multicastAddress;
    public static ConfigReader configReader;

    public static ArrayList<String> followerList = new ArrayList<>();

    public static void main(String[] args) {
        // lettura del file config
        System.out.println(
                "\n**************** Benvenuto in WINSOME ****************\n\nAttendi stiamo recuperando i tuoi dati...");
        try {
            configReader = new ConfigReader(CLIENT);
            setServerAddress();
            setServerPort();
            setServerRmiPort();
            setServerRmiRegistryName();
            setClientRmiPort();
            setServerRmiCallbackRegistryName();
            setMulticastPort();
            setMulticastAddress();
            try {
                loadingBar();
            } catch (InterruptedException e) {
                /* ignored */}
        } catch (IOException e) {
            System.err.println(
                    "Siamo spiacenti, impossible recuperare le informazioni dal file di configurazione\n***** (TT_TT) *****\n"
                            + e.getLocalizedMessage());
            return;// termina se non legge/crea il file di config
        }

        Socket socket;
        Scanner inputReader = new Scanner(System.in);
        boolean connectionState = true; // true se la connessione è on, false se la connessione è off

        //creo e avvio il thread che ascolta le notifiche di aggiornamento del wallet
        Thread walletNofierThread = new Thread(new WalletRewardNotifier(multicastAddress, multicastPort));
        walletNofierThread.start();

        //tentativo di collegamento con il server
        //debug.messaggioDiDebug("tentativo di connessione al server");
        connessionealserver:
        while(true){
            try {
                socket = new Socket(serverAddress, serverPort);
            } catch (IOException e) {
                //nel caso il serever fosse irraggiungibile o cadesse la connessione è possibile provare a ricollegarsi "Affrontado l'orco"
                System.out.println("Oh nooo!!!! (x.x)\nSembra che il server sia prigioniero di un grosso e spaventoso orco.\nTi senti coraggioso/a? [S/N]\n Digita S per ritentare, N per uscire");
                if(SharedMethods.readFromConsole(inputReader).equalsIgnoreCase("N")){
                    System.out.print("Hai deciso di dartela a gambe. (-^-)");
                    connectionState = false;
                }else if (SharedMethods.readFromConsole(inputReader).equalsIgnoreCase("S")){
                    System.out.println("Tentativo di riconnessione in corso...");
                }
            }
        }
        System.out.println("Congratulazioni "+"\\"+"_(*w*)_/ sei conesso al ServerWinsome.");
        try{
            Registry registry= LocateRegistry.getRegistry(serverAddress, serverRmiPort);
            RmiServiceInterface stub =(RmiServiceInterface) registry.lookup(serverRmiRegistryName);
        }catch(RemoteException e){
            e.printStackTrace();
            return;
        }
        String completeRequest = "";
        String op ="";
        NotifyEventInterface callback;

        try{
            /*il printwriter fa parte sempre della classe java.io e estende java.io.Writer,
             serve a stampare una rappresentazione formattata degli oggetti in un text-output stream, 
             la classe implementa i metodi di print della classe PrintStream, e non contine metodi per scrivere raw bytes
             il secondo argomento true serve a settare l'autoFlush, che però verra effettuato solo quando verrano chiamati metodi di print
             i metodi della classe PrintWriter non generano IOExceptions */
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while(true){
                //Ricevo il comando dall'utente se è vuoto lo ignoro
                completeRequest = SharedMethods.readFromConsole( inputReader);
                if(completeRequest.equals("")){
                    continue;
                }
                String[] splitCommandLine = completeRequest.split(" ");
                op = splitCommandLine[0]; // il primo elemnto dell'array splitCommandLine è l'operazione da eseguire
                //copio il resto degli argomenti in un'altro array
                String[] otherArgumentsInCommandLine = new String[splitCommandLine.length -1];
                System.arraycopy(splitCommandLine, 1, otherArgumentsInCommandLine, 0, (splitCommandLine.length-1));
                //effettuo la switch su op per vedere qual'è l'operazione richiesta dall'utente
                switch(op){
                    case "help":
                    help();
                        break;
                }
            }
        }catch(){

        }


    }

    // recupera l'indirizzo dal file di config
    public static void setServerAddress() {
        serverAddress = configReader.getConfigValue("ServerAddress");
        debug.messaggioDiDebug("serverAddress " + serverAddress);
    }

    // recupera la porta del server dal file di config
    public static void setServerPort() {
        serverPort = Integer.parseInt(configReader.getConfigValue("ServerPort"));
        debug.messaggioDiDebug("serverPort: " + serverPort);
    }

    // recupero la porta dell'interfaccia rmi che permette al client di registrarsi
    // dal file di config
    public static void setServerRmiPort() {
        serverRmiPort = Integer.parseInt(configReader.getConfigValue("RmiServerPort"));
        debug.messaggioDiDebug("serverRmiPort:" + serverRmiPort);
    }

    // recupero il nome del registry dell'interfaccia Rmi presente nel configFile
    public static void setServerRmiRegistryName() {
        serverRmiRegistryName = configReader.getConfigValue("ServerRmiRegistryName");
        debug.messaggioDiDebug("serverRmiREgistryName: " + serverRmiRegistryName);
    }

    // recupero dal file di config la porta dell'interfaccia che permette al server
    // diconoscere i cambiamenti nella lista follower
    public static void setClientRmiPort() {
        clientRmiPort = Integer.parseInt(configReader.getConfigValue("RmiClientCallbackPort"));
        debug.messaggioDiDebug("clientRmiPort: " + clientRmiPort);
    }

    // recupero del nome del registry dell'interfaccia RmiCallback sia presente nel
    // file di config
    public static void setServerRmiCallbackRegistryName() {
        serverRmiCallbackRegistryName = configReader.getConfigValue("RmiCallbackClientRegistryName");
        debug.messaggioDiDebug("serverRmiCallbackRegistryName:" + serverRmiCallbackRegistryName);
    }

    // recupero l'indirizzo multicast per le notifiche di aggiornamento del wallet
    public static void setMulticastPort() {
        multicastPort = Integer.parseInt(configReader.getConfigValue("MulticastPort"));
        debug.messaggioDiDebug("multicastPort: " + multicastPort);
    }

    // recupero dal file di config l'indirizzo multicast a cui il server invia le
    // notifiche di aggiornamento wallet
    public static void setMulticastAddress() {
        multicastAddress = configReader.getConfigValue("MulticastAddress");
        debug.messaggioDiDebug("multicastAddress: " + multicastAddress);
    }

    // metetodo che stampa il comando help
    public static void help() {
        System.out.println("Hai bidogno di aiuto?\nEccoti una clista dei comadi pronta per te:");
        System.out.println("help    Serve a mostrare questa lista, ma questo lo sai. :-)");
        System.out
                .println("login <username> <password>   Serve per effettuare il login nel magico mondo di Winsome.");
        System.out.println("logout   Serve per sloggare dal magico mondo di Winsome. :-(");
        System.out.println(
                "listuser   Serve a msotrarti tutti gli utenti che hanno passioni in comune con te.");
        System.out.println("listfollowers    Serve a mostrarti chi sono gli utenti che ti seguno.");
        System.out.println("listfollowing  Serve a mostrarti chi sono gli utenti che segui.");
        System.out.println("blog      Serve a mostrare tutti i post nel tuo blog.");
        System.out.println("showfeed   Serve a mostrare tutti i post nel tuo feed.");
        System.out.println(
                "wallet   Serve a farti vedere quanti wincoins hai accumulato nelle tue avventure qui su Winsome.");
        System.out.println("walletbtc    Serve a mostrarti quanti bitcoins sono i tuoi wincoins.");
        System.out.println("follow <username>   Serve per seguire un utente.");
        System.out.println("unfollow <username>  Serve per smettere di seguire un utente.");
        System.out.println("rewin <idpost>   Serve a rewinare(retwittare) un post nel tuo blog.");
        System.out.println("rate <idpost> <-1/+1>   Serve per upvotare(+1) o downvotare(-1) un post.");
        System.out.println("showpost <idpost>  Serve a mostrare il contenuto di un post.");
        System.out.println("post \"<titolo>\" \"<testo>\"   Serve per creare un post.");
        System.out.println("delete <idpost>  Serve per eliminare un post.");
        System.out.println("comment <idpost> <testo del commento> Serve per commentare un post.");
    }

    // metodo per la stampa della barra di caricamento
    public static void loadingBar() throws InterruptedException {
        String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
        String ANSI_RESET = "\u001B[0m";
        System.out.printf("Caricameneto: \t");

        System.out.printf("\r");
        System.out.printf("\t\t");
        for (int k = 0; k < 55; k++) {
            System.out.print(ANSI_YELLOW_BACKGROUND + " ");
            WinsomeClientMain m = new WinsomeClientMain();
            synchronized (m) {
                m.wait(79);
            }
        }
        System.out.println(ANSI_RESET + "\n\t\tCaricamento completato \\_(^w^)_/");
        return;
    }

}