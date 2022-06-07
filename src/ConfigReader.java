import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

//classe che legge i file di configurazione e li crea se non presenti

public class ConfigReader {
    // private static final DEBUG debug = new DEBUG();
    private static final String CONFIG_FOLDER = "config";// -> cartella dove vengono salvati i file di config
    private static final String SERVER_PATH = "ServerFile.proprieties"; // -> file config per il server
    private static final String CLIENT_PATH = "ClientFile.proprieties"; // -> file config per il client
    private final String filePath;
    private final Properties prop;

    // se il file di config server o client che sia non viene trovato sarà
    // automaticamente inizializzato

    public ConfigReader(boolean flag) throws IOException {
        if (flag) {
            // se è un server flag=true
            this.filePath = SERVER_PATH;
            // debug.messaggioDiDebug("filePath = "+filePath);
        } else {
            // se è un client
            this.filePath = CLIENT_PATH;
            // debug.messaggioDiDebug("filePath = "+filePath);

        }

        File configFile = new File(System.getProperty("user.dir"), CONFIG_FOLDER + "/" + this.filePath);
        // user.dir è directory corrente che è specificata dall'applicazione che lancia
        // l'applicazione
        if (!configFile.exists()) {
            boolean ignoredFlag = new File(CONFIG_FOLDER).mkdirs();
            /*
             * //DEBUG
             * if(ignoredFlag == true){
             * //debug.messaggioDiDebug("cartella creata correttemente");
             * }else{
             * //debug.messaggioDiDebug("cartella non creata");
             * }
             */
            OutputStream out = new FileOutputStream(CONFIG_FOLDER + "/" + this.filePath);
            this.prop = new Properties();
            if (flag == true) {
                // creazione del file di config del server i parametri sono:
                /*
                 * porta del server
                 * porta RMI del server
                 * nome registro RMI del server
                 * porta callback RMI del client
                 * nome registro callback RMI del client
                 * lunghezza massima del titolo del post
                 * lunghezza massima del corpo del post
                 * nome della valuta Winsome singolare
                 * nome della valuta Winsome plurale
                 * cifre decimali
                 * indirizzo multicast
                 * porta multicast
                 * intervallo di backup dei dati
                 * default causale di una trasazione
                 * Per l'Autore di un post->causale di una transazione relativa ad un premio
                 * Per in Curatore di un post->causale di una transazione relativa ad un premio
                 * percentuale del premio di un post dell'autore
                 * percentuale del premio di un post del curatore
                 * tempo che il RewardsManager dovrà attendere ogni volta che fa un controllo
                 * prima di farne un altro
                 * tempo (in millisecondi)prima di chiudere forzatamente la pool di thread
                 * ConnectionHandler
                 * data UNIX dell'ultimo controllo del RewardsManager
                 * contatore dell'ultimo post di WinSome
                 */
                this.prop.setProperty("ServerPort", "1511");
                this.prop.setProperty("RmiServerPort", "1115");
                this.prop.setProperty("ServerRmiRegistryName", "WinsomeServer");
                this.prop.setProperty("RmiCallbackClientPort", "1151");
                this.prop.setProperty("RmiCallbackClientRegistryName", "WinsomeCallbackServer");
                this.prop.setProperty("PostTitleMaxLenght", "50");
                this.prop.setProperty("PostTextMaxLenght", "500");
                this.prop.setProperty("CurrencyNameS", "Wincoin");
                this.prop.setProperty("CurrencyNameP", "Wincoins");
                this.prop.setProperty("DecimalPlace", "3");
                this.prop.setProperty("MulticastAddress", "239.255.32.32");
                this.prop.setProperty("MulticastPort", "4444");
                this.prop.setProperty("BackupInterval", "10000");
                this.prop.setProperty("DefaultReason", "NULL");
                this.prop.setProperty("AuthorReason", "Autore ricompensato per il post #{post}");
                this.prop.setProperty("CuratorReason", "Curatore ricompensato per il post #{post}");
                this.prop.setProperty("PercentageAuthor", "70");
                this.prop.setProperty("PercentageCurator", "30");
                this.prop.setProperty("RewardChecktimeout", "15000");
                this.prop.setProperty("TimeoutBeforeShutdown", "40000");
                this.prop.setProperty("LastRewardUnixCheck", "0");
                this.prop.setProperty("LastPostId", "0");
                // debug.messaggioDiDebug("file config serever creato correttamnte");
            } else {
                // creazione del file di config per il client
                /*
                 * Indirizzo del server
                 * porta del server
                 * porta rmi del server
                 * porta callback rmi del server
                 * porta callback rmi del client
                 * nome del registro Rmi del server
                 * nome del registro callback rmi
                 * indirizzo multicast per ricevere le notifiche
                 * porta multicast per le notifiche
                 */
                this.prop.setProperty("ServerAddress", "localhost");
                this.prop.setProperty("ServerPort", "1511");
                this.prop.setProperty("RmiServerPort", "1115");
                this.prop.setProperty("RmiClientCallbackPort", "1151");
                this.prop.setProperty("ServerRmiRegistryName", "WinsomeServer");
                this.prop.setProperty("RmiCallbackClientRegistryName", "WinsomeCallbackServer");
                this.prop.setProperty("MulticastAddress", "239.255.32.32");
                this.prop.setProperty("MulticastPort", "4444");
                // debug.messaggioDiDebug("file config client creato correttamnte");
            }
            this.prop.store(out, "Winsome Configuration File");
            out.close();
            // debug.messaggioDiDebug("file storato correttamnte");

        } else {
            InputStream inStream = new FileInputStream(CONFIG_FOLDER + "/" + this.filePath);
            this.prop = new Properties();
            this.prop.load(inStream);
            // debug.messaggioDiDebug("file config caricato correttamnte");
            inStream.close();
        }

    }

    // recupera informazioni dal file di config se non esiste nulla ritorna NULL
    public String getConfigValue(String key) {
        if (this.prop != null) {
            return this.prop.getProperty(key, null);
        }
        return null;
    }

    // Salva un'aggiunta o una modifica al file di configurazione
    public void changeSaveConfig(String key, String value) {
        OutputStream out;
        try {
            out = new FileOutputStream(CONFIG_FOLDER + "/" + filePath);
            prop.setProperty(key, value);
            try {
                prop.store(out, "Winsome Configuration File");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}