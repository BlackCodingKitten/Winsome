import java.io.IOException;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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

    public static void main(String[] args) throws NotBoundException {
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

        Socket socket = null;
        Scanner inputReader = new Scanner(System.in);
        boolean connectionState = true; // true se la connessione è on, false se la connessione è off
        RmiCallbackInterface server = null;
        NotifyEventInterface cInterface;
        String nickname = null;

        // creo e avvio il thread che ascolta le notifiche di aggiornamento del wallet
        Thread walletNofierThread = new Thread(new WalletRewardNotifier(multicastAddress, multicastPort));
        walletNofierThread.start();

        // tentativo di collegamento con il server
        // debug.messaggioDiDebug("tentativo di connessione al server");
        loop: while (true) {
            try {
                socket = new Socket(serverAddress, serverPort);
            } catch (IOException e) {
                // nel caso il serever fosse irraggiungibile o cadesse la connessione è
                // possibile provare a ricollegarsi "Affrontado l'orco"
                System.out.println(
                        "Oh nooo!!!! (x.x)\nSembra che il server sia prigioniero di un grosso e spaventoso orco.\nTi senti coraggioso/a? [S/N]\n Digita S per ritentare, N per uscire");
                if (SharedMethods.readFromConsole(inputReader).equalsIgnoreCase("N")) {
                    System.out.print("Hai deciso di dartela a gambe. (-^-)");
                    connectionState = false;
                } else if (SharedMethods.readFromConsole(inputReader).equalsIgnoreCase("S")) {
                    System.out.println("Tentativo di riconnessione in corso...");
                }
            }
            System.out.println("Congratulazioni " + "\\" + "_(*w*)_/ sei conesso al ServerWinsome.");
            RmiServiceInterface stub;
            try {
                Registry registry = LocateRegistry.getRegistry(serverAddress, serverRmiPort);
                stub = (RmiServiceInterface) registry.lookup(serverRmiRegistryName);
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            }
            String completeRequest = "";
            String op = "";
            NotifyEventInterface callback = null;
            String fromServer;
            boolean successRequest = true;// true se la request è eseguita correttamente false se la request fallisce

            try {
                /*
                 * il printwriter fa parte sempre della classe java.io e estende java.io.Writer,
                 * serve a stampare una rappresentazione formattata degli oggetti in un
                 * text-output stream,
                 * la classe implementa i metodi di print della classe PrintStream, e non
                 * contine metodi per scrivere raw bytes
                 * il secondo argomento true serve a settare l'autoFlush, che però verra
                 * effettuato solo quando verrano chiamati metodi di print
                 * i metodi della classe PrintWriter non generano IOExceptions
                 */
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    // Ricevo il comando dall'utente se è vuoto lo ignoro
                    System.out.println("WinsomeServer è in attesa di istruzioni...");
                    if (successRequest == false) {
                        // leggo l'input utente e lo ignoro
                        SharedMethods.readFromConsole(inputReader);
                        if (callback != null) {
                            // se non ci fosse questo controllo il client terminerebbe
                            UnicastRemoteObject.unexportObject(callback, false);
                        }
                        successRequest = true;
                    } else {
                        completeRequest = SharedMethods.readFromConsole(inputReader);
                        debug.messaggioDiDebug("Richiesta fatta al server: " + completeRequest);
                        if (completeRequest.equals("")) {
                            continue;
                        }
                    }

                    String[] splitCommandLine = completeRequest.split(" ");
                    op = splitCommandLine[0]; // il primo elemnto dell'array splitCommandLine è l'operazione da eseguire
                    // copio il resto degli argomenti in un'altro array
                    String[] otherArgumentsInCommandLine = new String[splitCommandLine.length - 1];
                    System.arraycopy(splitCommandLine, 1, otherArgumentsInCommandLine, 0,
                            (splitCommandLine.length - 1));
                    // effettuo la switch su op per vedere qual'è l'operazione richiesta dall'utente
                    switch (op) {
                        case "help":
                            help();
                            break;
                        case "logout":
                            // operazione di logout
                            SharedMethods.sendToServer(out, completeRequest);
                            fromServer = SharedMethods.readFromServer(in);
                            // se il logout ha avuto successo lato sever
                            if (fromServer.equalsIgnoreCase("OK")) {
                                System.out.println("Logout avvenuto con successo.\nArrivederci!");
                                server.callbackUnregister(nickname);
                                UnicastRemoteObject.unexportObject(callback, false);
                                callback = null;
                            } else {
                                // Se il logout non ha avuto successo:
                                System.out.println(
                                        "Impossibile effettuare il logout, voglio tenerti qui con me per sempre\nMUAHAHAHA }:-) ");
                            }
                            break;
                        case "login":
                            // operazione di login
                            SharedMethods.sendToServer(out, completeRequest);
                            fromServer = SharedMethods.readFromServer(in);
                            if (fromServer.equalsIgnoreCase("OK")) {
                                nickname = otherArgumentsInCommandLine[0];
                                // registrazione al servizio notifiche per la lista follower

                                Registry callbackRegistry = LocateRegistry.getRegistry(serverAddress, clientRmiPort);
                                server = (RmiCallbackInterface) callbackRegistry.lookup(serverRmiCallbackRegistryName);
                                callback = new NotifyEvent();
                                cInterface = (NotifyEventInterface) UnicastRemoteObject.exportObject(callback, 0);

                                server.callbackRegister(nickname, cInterface);
                                followerList = stub.followerList(nickname, otherArgumentsInCommandLine[1]);
                                System.out.println("Bentornato nel magico mondo di Winsome " + nickname
                                        + " siamo lieti di rivederti.");

                            } else {
                                System.out.println("Ops, sembra che qualcosa sia andato storto (TT.TT) " + fromServer);
                            }
                            break;
                        case "forcedexit":
                            // operazione di uscita forzata
                            socket.close();
                            out.close();
                            in.close();
                            break loop;
                        case "register":
                            // operazione di register
                            // per prima cosa controllo che siano stati inseriti nome utente, password e max
                            // 5 tag
                            if (otherArgumentsInCommandLine.length > 7) {
                                System.out.println(
                                        "Impossibile eseguire la registrazione di un nuovo utente.\nPer informazioni su come eseguire una corretta registrazione digita \"help\".");
                                break;
                            } else {
                                // copio la lista dei tag dall'array otherArgumentInCommandLine
                                ArrayList<String> tags = new ArrayList<>(Arrays.asList(otherArgumentsInCommandLine)
                                        .subList(2, (otherArgumentsInCommandLine.length - 1)));
                                boolean flag = stub.registerNewUser(otherArgumentsInCommandLine[0],
                                        otherArgumentsInCommandLine[1], tags);
                                if (flag) {
                                    System.out.println("Congratulazioni " + otherArgumentsInCommandLine[0]
                                            + " la tua registrazione ha avuto successo.");
                                } else {
                                    System.out.println(
                                            "Siamo spiacenti non è possibile concludere la fase di registrazione.\nRitenta cambiando username");
                                }
                                break;
                            }
                        case "listfollowers":
                            // operazione gestita dal client perchè la lista dei follower è salvata in
                            // locale
                            if (nickname != null) {
                                if (followerList.size() > 0) {
                                    System.out.println("Lista dei tuoi followers:");
                                    for (String follower : followerList) {
                                        System.out.println(follower);
                                        break;
                                    }
                                } else {
                                    System.out.println("Nessuno ti segue. Sei nuovo o sei solo antipatico?\n****** "
                                            + "\\" + "_(a.a)_/ ******");
                                    break;
                                }
                            } else {
                                System.out.println(
                                        "Non posso sapere chi ti segue se non so chi sei, effettua il login prima.");
                                break;
                            }
                        case "listusers":
                        case "listfollowing":
                        case "blog":
                        case "showfeed":
                        case "wallet":
                        case "walletbtc":
                        case "follow":
                        case "unfollow":
                        case "rewin":
                        case "rate":
                        case "showpost":
                        case "comment":
                        case "delete":
                        case "post":
                            // tutte queste altre operazioni vengono gestite direttamente dal server senza
                            // bisogno di ulteriori controlli
                            SharedMethods.sendToServer(out, completeRequest);
                            System.out.println("WinsomeServer: " + SharedMethods.readFromServer(in));
                            break;
                        default:
                            // nessun comando riconosciuto
                            System.out.println("Operazione non riconosciuta, per favore riprova.");
                            SharedMethods.readFromConsole(inputReader);

                    }// end switch

                } // end while richieste utente
            } catch (IOException e) {
                // persa la connessione col server
                System.out.println(
                        "Oh no l'orco cattivo è tornato e si è preso il nostro server.\nVuoi affrontarlo di nuovo? [S/N]");
                if (SharedMethods.readFromConsole(inputReader).equalsIgnoreCase("S")) {
                    if (op.equalsIgnoreCase("login") || op.equalsIgnoreCase("listfollowers")
                            || op.equalsIgnoreCase("register") || op.equalsIgnoreCase("logout")
                            || op.equalsIgnoreCase("forcedexit"))
                        ;
                    successRequest = false;
                    // in questo modo se l'ultima richiesta è login o register o logout o
                    // listfollowers o un'uscita forzata viene ricopiata direttamente senza che
                    // l'utente debba riscrivere il comando
                } else {
                    connectionState = false;
                    System.out.println("Le nostre armate sono dalla tua parte.");
                    break;
                }
            } // end catch
        } // end loop
    }// end main

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
                .println("register <username> <password> <elnenca max 5 tag>       Serve per registrare nuovi utenti");
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
        System.out.println("forcedexit      Serve per forzare l'uscita dal social");
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