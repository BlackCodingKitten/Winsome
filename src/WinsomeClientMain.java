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

    public static String serverAddress;
    public static int serverPort;
    public static int serverRmiPort;
    public static int clientRmiPort;
    public static String serverRmiRegistryName;
    public static String serverRmiCallbackRegistryName;
    public static int multicastPort;
    public static String multicastAddress;
    public static ConfigReader configReader;

    public static ArrayList<String> followerList = new ArrayList<>();ù

    public static void main(String[] args) {
        // lettura del file config
        System.out.println("***** Benvenuto in WINSOME *****\nAttendi stiamo recuperando i tuoi dati...");
        try {
            configReader = new ConfigReader(CLIENT);
            setServerAddress();
            setServerPort();
            setServerRmiPort();
            setServerRmiRegistryName();
            setClientRmiPort();
            setServerRmiCallbackRegistryName();
            setMulticastPort();
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
            return;// termina se non legge/crea il file di config
        }
    }

    // recupera l'indirizzo dal file di config
    public static void setServerAddress() {
        serverAddress = configReader.getConfigValue("ServerAdress");
    }

    // recupera la porta del server dal file di config
    public static void setServerPort() {
        serverPort = Integer.parseInt(configReader.getConfigValue("ServerPort"));
    }

    // recupero la porta dell'interfaccia rmi che permette al client di registrarsi
    // dal file di config
    public static void setServerRmiPort() {
        serverRmiPort = Integer.parseInt(configReader.getConfigValue("RmiServerPort"));
    }

    // recupero il nome del registry dell'interfaccia Rmi presente nel configFile
    public static void setServerRmiRegistryName() {
        serverRmiRegistryName = configReader.getConfigValue("ServerRmiRegistryName");
    }

    // recupero dal file di config la porta dell'interfaccia che permette al server
    // diconoscere i cambiamenti nella lista follower
    public static void setClientRmiPort() {
        clientRmiPort = Integer.parseInt(configReader.getConfigValue("RmiClientCallbackPort"));
    }

    // recupero del nome del registry dell'interfaccia RmiCallback sia presente nel
    // file di config
    public static void setServerRmiCallbackRegistryName() {
        serverRmiCallbackRegistryName = configReader.getConfigValue("RmiCallbackClientRegistryName");
    }

    // recupero l'indirizzo multicast per le notifiche di aggiornamento del wallet
    public static void setMulticastPort() {
        multicastPort = Integer.parseInt(configReader.getConfigValue("MulticastPort"));
    }

    // recupero dal file di config l'indirizzo multicast a cui il server invia le
    // notifiche di aggiornamento wallet
    public static void setMulticastAddress() {
        multicastAddress = configReader.getConfigValue("MulticastAddress");
    }

}