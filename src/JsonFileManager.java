import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;


//Classe che si occupa della gestione dei file json

public class JsonFileManager {
    private final String FolderName = "config/jsonFile";

    private static final String UserPath = "user.json";
    private static final String PostPath = "post.json";
    private static final String WalletPath = "wallet.json";
    private static final String FollowerPath = "follower.json";
    private static final String FollowingPath = "following.json";

    private final Gson gson;

    // costruttore della classe manager
    public JsonFileManager() {
        this.gson = new Gson();
        boolean temp = createBackupFile(FolderName + "/" + UserPath);
        boolean temp2 = createBackupFile(FolderName + "/" + WalletPath);
        boolean temp3 = createBackupFile(FolderName + "/" + FollowingPath);
        boolean temp4 = createBackupFile(FolderName + "/" + FollowerPath);
        boolean temp5 = createBackupFile(FolderName + "/" + PostPath);
        if (temp && temp2 && temp3 && temp4 && temp5) {
            System.err.println("Errore nei file di backup dei dati.");
        }

    }

    // metodo per caricare tutti i file in una volta sola
    public void loadBackupFile(SocialManager socialManager) {
        loadUserFile(socialManager);
        loadPostFile(socialManager);
        loadWalletFile(socialManager);
        loadFollowerFile(socialManager);
        loadFollowingFile(socialManager);
    }

    // carica il file con i dati dei follower
    public void loadFollowerFile(SocialManager s) {
        String fromFile = getFromFile(FolderName + "/" + FollowerPath);
        if (fromFile == null || fromFile.equals("")) {
            System.out.println("File dati dei Follower vuoto.");
            return;
        } else {
            // type è la superinterfaccia comune per tutti i tipi, senza questa la jvm non
            // sarebbe in grado di ricavare lòa struttura esatta degli oggetti serializzati
            Type listafollower = new TypeToken<ConcurrentHashMap<String, HashSet<String>>>() {
            }.getType();
            s.jsonFollowerList(gson.fromJson(fromFile, listafollower));
        }

    }

    // carica il file con i dati dei seguiti
    public void loadFollowingFile(SocialManager s) {
        String fromFile = getFromFile(FolderName + "/" + FollowingPath);
        if (fromFile == null || fromFile.equals("")) {
            System.out.println("File dati dei Seguiti vuoto.");
            return;
        } else {
            // type è la superinterfaccia comune per tutti i tipi, senza questa la jvm non
            // sarebbe in grado di ricavare lòa struttura esatta degli oggetti serializzati
            Type listafollowing = new TypeToken<ConcurrentHashMap<String, HashSet<String>>>() {
            }.getType();
            s.jsonFollowingList(gson.fromJson(fromFile, listafollowing));
        }

    }

    // carica il file con i dati dei wallet
    public void loadWalletFile(SocialManager s) {
        String fromFile = getFromFile(FolderName + "/" + WalletPath);
        if (fromFile == null || fromFile.equals("")) {
            System.out.println("File dati Wallet vuoto.");
            return;
        } else {
            // type è la superinterfaccia comune per tutti i tipi, senza questa la jvm non
            // sarebbe in grado di ricavare lòa struttura esatta degli oggetti serializzati
            Type listaWallet = new TypeToken<ConcurrentHashMap<String, Wallet>>() {
            }.getType();
            s.jsonWalletList(gson.fromJson(fromFile, listaWallet));
        }

    }

    // carica il file con i dati dei post
    public void loadPostFile(SocialManager s) {
        String fromFile = getFromFile(FolderName + "/" + PostPath);
        if (fromFile == null || fromFile.equals("")) {
            System.out.println("File dati Post vuoto.");
            return;
        } else {
            // type è la superinterfaccia comune per tutti i tipi, senza questa la jvm non
            // sarebbe in grado di ricavare lòa struttura esatta degli oggetti serializzati
            Type listaPost = new TypeToken<ConcurrentHashMap<Integer, Post>>() {
            }.getType();
            s.jsonPostList(gson.fromJson(fromFile, listaPost));
        }

    }

    // carica il file con i dati degli user
    public void loadUserFile(SocialManager s) {
        String fromFile = getFromFile(FolderName + "/" + UserPath);
        if (fromFile == null || fromFile.equals("")) {
            System.out.println("File dati User vuoto.");
            return;
        } else {
            // type è la superinterfaccia comune per tutti i tipi, senza questa la jvm non
            // sarebbe in grado di ricavare lòa struttura esatta degli oggetti serializzati
            Type listaUtenti = new TypeToken<ConcurrentHashMap<String, User>>() {
            }.getType();
            s.jsonUserList(gson.fromJson(fromFile, listaUtenti));
        }

    }

    // metodo per leggere dinamicamnete un file
    private String getFromFile(String path) {
        ReadableByteChannel channel;
        try {
            channel = Channels.newChannel(new FileInputStream(path));
        } catch (Exception e) {
            System.err.println("Path non valida per il file.");
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
        String containFile = ""; // -> contiene il file alla fine del metodo
        buffer.clear();
        try {
            while (channel.read(buffer) >= 0) {// -> read ritorna 0 o -1 quando ha finito di leggere lo stream
                buffer.flip();// -> preparo la lettura
                containFile = getJson(containFile, buffer); // -> aggiungo la stringa
                buffer.compact();// -> pulisco il buffer

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // mi assicuro che tutti i dati siano stati letti
        buffer.flip();
        while (buffer.hasRemaining()) {
            containFile = getJson(containFile, buffer);
        }

        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return containFile;// a questo punto nella stringa è inserito tutto il file

    }

    // metodo che si salva mano a mano 1 byte per volta su una stringa
    private String getJson(String append, ByteBuffer buffer) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(append);
        while (buffer.hasRemaining()) {
            stringBuilder.append((char) buffer.get());
        }

        return stringBuilder.toString();
    }

    private boolean createBackupFile(String path) {
        boolean ignored = new File(FolderName).mkdirs(); // creo la cartella dove trenere i file se non esiste già
        if(ignored){
            //DEBUG.messaggioDiDebug("il file è stato creato");
        }else{
           // DEBUG.messaggioDiDebug("il file già esisteva");
        }
        try {
            File f = new File(path);
            if (!f.exists() || !f.createNewFile()) {
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
           
            return false;
        }
    }

    /*************************
     * Metodi per salvare sui file
     *********************/

    // Stessa struttura creo un metodo che faccia tutti i singoli save dei file
    public void save(SocialManager socialManager) throws IOException {
        fileSaver(FolderName + "/" + UserPath, socialManager.getUserList());
        fileSaver(FolderName + "/" + PostPath, socialManager.getPostList());
        fileSaver(FolderName + "/" + WalletPath, socialManager.getWalletList());
        fileSaver(FolderName + "/" + FollowerPath, socialManager.getFollowersList());
        fileSaver(FolderName + "/" + FollowingPath, socialManager.getFollowingsList());

    }

    public void fileSaver(String path, Object list) throws IOException {
        String data = gson.toJson(list);
        WritableByteChannel file = Channels.newChannel(new FileOutputStream(path));
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.getBytes().length);
        buffer.put(data.getBytes());
        buffer.flip();
        file.write(buffer);
        file.close();
    }
}