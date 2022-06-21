import java.util.Set;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Color.ColoredText;

/*Questa classe è sostanzialmente il cervello operante dietro Winsome
 implementa tutte le operazioni di risposta al client tramite il ConnectioHandler */

import CustomizedException.*;

public class SocialManager {
    // private static final DEBUG d = new DEBUG();
    /*
     * Necessita di leggere le informazioni sullo stato del social comequelle
     * relative all'id corrente del post
     */
    private final ConfigReader configReader;

    // sttributi specifici del social
    private static final String WINCOIN = "Wincoin";
    private static final String WINCOINS = WINCOIN + "s";
    private static final int maxTitleLength = 50;
    private static final int maxTextLength = 300;

    private ConcurrentHashMap<String, User> userList;
    private ConcurrentHashMap<Integer, Post> postList;
    private ConcurrentHashMap<String, Wallet> walletList;
    private ConcurrentHashMap<String, HashSet<String>> followersList;// -> lista di quelli che seguoni l'utente
    private ConcurrentHashMap<String, HashSet<String>> followingList;// ->lista di quelli che un utente segue

    public AtomicInteger currentIdPost;

    // metodo costruttore della classe SocialManager
    public SocialManager(ConfigReader configReader) {
        userList = new ConcurrentHashMap<>();
        followingList = new ConcurrentHashMap<>();
        postList = new ConcurrentHashMap<>();
        walletList = new ConcurrentHashMap<>();
        followersList = new ConcurrentHashMap<>();
        this.configReader = configReader;
        this.currentIdPost = setCurrentIdPost();
    }

    // metodo per fare l'unfollow di un utente
    public void unfollow(String username, String following) throws InvalidOperationException, UserNotFoundException {
        DEBUG.messaggioDiDebug("Sono in unfollow nel social manager");
        if (username.equalsIgnoreCase(following)) {
            throw new InvalidOperationException();
        }
        DEBUG.messaggioDiDebug("userneame è diverso da following");
        if (!userList.containsKey(following)) {
            throw new UserNotFoundException();
        }
        DEBUG.messaggioDiDebug("l'utente seguito esiste");
        if (!followersList.getOrDefault(following, new HashSet<String>()).contains(username)) {
            throw new InvalidOperationException();
        }
        DEBUG.messaggioDiDebug("l'utente è contenuto nella lista dei seguiti");
        DEBUG.messaggioDiDebug("Entro in remove follower");
        removeFollower(following, username);
        DEBUG.messaggioDiDebug("esco da remove follower ");
        DEBUG.messaggioDiDebug("entro in remove following");
        removeFollowing(username, following);
        DEBUG.messaggioDiDebug("Esco da remove following");
        try {
            WinsomeServerMain.fileManager.fileSaver("config/jsonFile/following.json", followingList);
        } catch (IOException e) {
            DEBUG.messaggioDiDebug("impossibile salvare i following su file");
        }
    }

    // metodo get della lista dei post
    public ConcurrentHashMap<Integer, Post> getPostList() {
        return this.postList;
    }

    // metodo get della lista dei follower
    public ConcurrentHashMap<String, HashSet<String>> getFollowersList() {
        return this.followersList;
    }

    // metodo get della lista dei seguiti
    public ConcurrentHashMap<String, HashSet<String>> getFollowingsList() {
        return this.followingList;
    }

    // metodo get della UserList
    public ConcurrentHashMap<String, User> getUserList() {
        return this.userList;
    }

    // metodo get della wallet list
    public ConcurrentHashMap<String, Wallet> getWalletList() {
        return this.walletList;
    }

    // leggo il file di config per ottenre l'ultimo post id registrato dal sistema
    public AtomicInteger setCurrentIdPost() {
        return new AtomicInteger(Integer.parseInt(this.configReader.getConfigValue("CurrentPostId")));
    }

    // salvo o l'id dell'ultimo post nel file doi config
    public void saveCurrentIdPostOnConfigFile() {
        int i = currentIdPost.intValue();
        this.configReader.changeSaveConfig("CurrentPostId", String.valueOf(i));
    }

    // metodo per vedere se un post è nel feed
    public boolean isPostInFeed(int id, String username) {
        HashSet<Post> userFeed = getUserFeed(username);
        for (Post post : userFeed) {
            if (id == post.getpostId()) {
                return true;
            }
        }
        return false;
    }

    // metodo per eliminare un post
    public void deletePost(int id, String username) throws PostNotFoundException, InvalidOperationException {
        Post post = postList.get(id);

        if (post == null) {
            // se il post non esiste
            throw new PostNotFoundException();
        } else {
            DEBUG.messaggioDiDebug(post.getOwner());
            DEBUG.messaggioDiDebug(username);
            if (!post.getOwner().equalsIgnoreCase(username)) {
                // se l'utente che vuole eliminare il post non è l'autore
                throw new InvalidOperationException();
            }
        }

        postList.remove(id);
    }

    // metodo per recuperare il feed di uno specifico utente
    public HashSet<Post> getUserFeed(String username) {

        HashSet<Post> postInFeed = new HashSet<>();
        HashSet<Post> blog;
        HashSet<String> userFolloweds = getFollowedByUser(username);
        for (String nickname : userFolloweds) {
            blog = getBlog(nickname);
            if (blog == null) {
                continue;
            } else {
                postInFeed.addAll(blog);
            }
        }
        return postInFeed;
    }

    // metodo per vedere la lista degli utenti seguiti da un'utente specifico
    public HashSet<String> getFollowedByUser(String username) {
        HashSet<String> followeds = followingList.get(username);
        if (followeds != null) {
            return followeds;
        } else {
            return new HashSet<>();
        }
    }

    // metodo per prendere il blog di un utente
    public HashSet<Post> getBlog(String username) {
        HashSet<Post> blog = new HashSet<>();
        for (Post p : postList.values()) {
            if (p.getOwner().equalsIgnoreCase(username) || p.isNotUserRewinedPost(username)) {
                blog.add(p);
            }
        }
        // d.messaggioDiDebug("Raccolti i post dell'utente");
        return blog;
    }

    // metodo che restituisce la lista di post di un determinato autore
    public HashSet<Post> getAllUserPost(String username) {
        HashSet<Post> allPostByUser = new HashSet<>();
        for (Post post : postList.values()) {
            if (post.getOwner().equalsIgnoreCase(username)) {
                allPostByUser.add(post);
            }
        }
        // d.messaggioDiDebug("Tutti i post di un utente");
        return allPostByUser;
    }

    // metdo che permette di avere la lista di tutti gli utenti con un determinato
    // tag
    public HashSet<User> getUserByTag(String tag) {
        HashSet<User> uList = new HashSet<>();
        for (User u : userList.values()) {
            if (u.getTags().contains(tag)) {
                uList.add(u);
            }
        }
        return uList;
    }

    // metodo che permette di avere una lista di utenti con tag in comune tra i tuoi
    public HashSet<User> getCommonTagsUsers(Set<String> tags) {
        HashSet<User> userCommonTags = new HashSet<>();

        for (String tag : tags) {
            HashSet<User> tempList = getUserByTag(tag);
            userCommonTags.addAll(tempList);
        }
        return userCommonTags;
    }

    // metodo che prende uno specifico post dalla lista
    public Post getPost(int id) {
        return postList.get(Integer.valueOf(id));
    }

    // metodo per creare un post -> ritorna l'd del post
    public int createNewPost(String username, String title, String text)
            throws PostLengthException, UserNotFoundException {
        // controllo che l'username inserito sia registrato
        if (!userList.containsKey(username)) {
            throw new UserNotFoundException();
        }
        if (title.length() > maxTitleLength || text.length() > maxTextLength) {
            throw new PostLengthException();
        }
        int newId = currentIdPost.getAndIncrement();
        postList.putIfAbsent(newId, new Post(username, title, text, newId));
        return newId;
    }

    // metodo per aggiungere un commento ad un post
    public void commentPost(String username, int id, String comment)
            throws PostNotFoundException, PostNotInFeedException {
        Post post = postList.get(id);
        if (post == null) {
            DEBUG.messaggioDiDebug("il post non esiste.");
            // post non trovato
            throw new PostNotFoundException();
        } else {
            DEBUG.messaggioDiDebug("il post esiste");
            if (!isPostInFeed(id, username)) {
                DEBUG.messaggioDiDebug("il post non è nel feed");
                // post non in feed
                throw new PostNotInFeedException();
            } else {
                post.addNewComment(username, comment);
                DEBUG.messaggioDiDebug("Post commentato con: " + comment);
            }
        }
    }

    // metodo per votare un post
    public void ratePost(String username, int id, int value) throws UserNotFoundException, InvalidVoteValueException,
            PostNotFoundException, PostNotInFeedException, InvalidOperationException, SameUserException {
        if (!userList.containsKey(username)) {
            // username non presente nella lista di quelli registrati
            throw new UserNotFoundException();
        }
        if (!Vote.validVode(value)) {
            // se il valore del voto non è 1 o -1
            throw new InvalidVoteValueException();
        }
        Post ratePost = postList.get(id);
        if (ratePost == null) {
            // il post che si vuole votare non esiste
            throw new PostNotFoundException();
        }
        if (ratePost.getOwner().equalsIgnoreCase(username)) {
            // non puoi votare il tuo stesso post
            throw new SameUserException();
        }
        if (!isPostInFeed(id, username)) {
            // se il post non è nel feed
            throw new PostNotInFeedException();
        }
        if (ratePost.getVoteByUser(username) != null) {
            // se l'utente ha già votato il post
            // System.out.println(ColoredText.ANSI_RED + "Hai gia' votato questo post." +
            // ColoredText.ANSI_RESET);
            throw new InvalidOperationException();
        }
        // se nessuno degli if prima ha lanciato eccezioni aggiungo il voto alla lista
        // dei voti del post
        ratePost.addNewVote(username, value);

    }

    // metodo che stampa un post formattato
    public String formattedPost(int id) {
        Post toFormatt = postList.get(id);
        if (toFormatt == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ColoredText.ANSI_WHITE_BACKGROUND + ColoredText.ANSI_PURPLE + "Post " + id
                + ColoredText.ANSI_RESET + "\n");
        stringBuilder.append(ColoredText.ANSI_PURPLE + "Titolo del post:\t" + toFormatt.getTitle().toUpperCase()
                + ColoredText.ANSI_RESET + "\n");
        stringBuilder.append(ColoredText.ANSI_PURPLE + "Da: " + ColoredText.ANSI_RESET + toFormatt.getOwner()
                + ColoredText.ANSI_PURPLE + "\tIn Data: " + ColoredText.ANSI_RESET);
        stringBuilder.append(toFormatt.getDate() + "\n");
        stringBuilder.append(ColoredText.ANSI_PURPLE + "Testo: " + ColoredText.ANSI_WHITE_BACKGROUND
                + toFormatt.getText() + ColoredText.ANSI_RESET + "\n\n");
        stringBuilder.append(
                ColoredText.ANSI_PURPLE + "UpVote: " + ColoredText.ANSI_RESET + toFormatt.getNumUpperVotes() + "\t");
        stringBuilder.append(
                ColoredText.ANSI_PURPLE + "DownVote: " + ColoredText.ANSI_RESET + toFormatt.getNumDownVotes() + "\n");
        HashSet<Comment> commentList = toFormatt.getComments();
        stringBuilder.append(String.valueOf(ColoredText.ANSI_PURPLE + commentList.size()) + ColoredText.ANSI_RESET
                + " utenti hanno commentato questo post.");
        stringBuilder.append("\n");
        for (Comment c : commentList) {
            stringBuilder.append(ColoredText.ANSI_PURPLE + c.getOwner() + ":   " + ColoredText.ANSI_RESET);
            stringBuilder.append(c.getText() + "\n");
        }

        return stringBuilder.toString();

    }

    // metodo per fare il rewin di un post
    public void rewinPost(String username, int id)
            throws UserNotFoundException, PostNotFoundException, PostNotInFeedException, InvalidOperationException,
            SameUserException {
        if (!userList.containsKey(username)) {
            throw new UserNotFoundException();
        }
        DEBUG.messaggioDiDebug("L'utente che fa la rewin esiste");
        Post post = postList.getOrDefault(id, null);
        if (post == null) {
            DEBUG.messaggioDiDebug("il post non esiste");
            throw new PostNotFoundException();
        }
        DEBUG.messaggioDiDebug("il post esiste");
        if (post.getOwner().equalsIgnoreCase(username)) {
            throw new SameUserException();
        }
        DEBUG.messaggioDiDebug("l'utente che fa la rewin NON è l'autore del post");
        if (!isPostInFeed(id, username)) {
            throw new PostNotInFeedException();
        }
        DEBUG.messaggioDiDebug("il post è nel feed");
        DEBUG.messaggioDiDebug("entro  in add rewin nella classe post");
        boolean flag = post.addRewineUser(username);// metodo eseguito dalla classe post
        if (!flag) {
            // se ritorno false vuol dire che ho già fatto la rewin al post
            throw new InvalidOperationException();
        }
        DEBUG.messaggioDiDebug("rewine completato correttamente nel social media manager");

    }

    // metoodo per seguire un'altro utente
    public void follow(String follower, String user)
            throws UserNotFoundException, InvalidOperationException, SameUserException {
        DEBUG.messaggioDiDebug("sono dentro follow nel socialmanager");
        if (follower.equalsIgnoreCase(user)) {
            // d.messaggioDiDebug("L'utente sta cercando si seguirsi da solo");
            throw new SameUserException();
        }
        DEBUG.messaggioDiDebug("i due utenti non sono uguali");
        if (!userList.containsKey(user)) {
            throw new UserNotFoundException();
        }
        DEBUG.messaggioDiDebug("l'utente che si vuole seguire esiste");
        if (followingList.getOrDefault(follower, new HashSet<String>()).contains(user)) {
            DEBUG.messaggioDiDebug("segui già l'utente");
            throw new InvalidOperationException();
        }
        DEBUG.messaggioDiDebug("prima delle operazioni interne di follow");
        addNewFollower(user, follower);
        DEBUG.messaggioDiDebug("ho eseguito add new follower");
        addNewFollowing(follower, user);
        DEBUG.messaggioDiDebug("ho eseguito add new following");
        // salvo nel file json il nuovo follower
        try {
            WinsomeServerMain.fileManager.fileSaver("config/jsonFile" + "/" + "follower.json", followersList);
            WinsomeServerMain.fileManager.fileSaver("config/jsonFile" + "/" + "following.json", followingList);
            DEBUG.messaggioDiDebug("ho salvato nei file json.");
        } catch (IOException e) {
            /* ignored tanto c'è il backup automatico */
        }
    }

    // metodo per aggiungere un utente alla follower list
    public void addNewFollower(String user, String follower) {
        DEBUG.messaggioDiDebug("sono sentro add new follower");
        if (followersList.containsKey(user)) {
            DEBUG.messaggioDiDebug("La lista esiste ");
            followersList.get(user).add(follower);
            DEBUG.messaggioDiDebug("ho aggiunto");
        } else {
            DEBUG.messaggioDiDebug("la lista non esisteva");

            HashSet<String> newList = new HashSet<>();
            newList.add(follower);
            followersList.put(user, newList);

            DEBUG.messaggioDiDebug("ho aggiunto");
        }

    }

    // metodo che aggiorna la lista following
    public void addNewFollowing(String follower, String user) {
        if (followingList.containsKey(follower)) {
            DEBUG.messaggioDiDebug("la lista di following esiste");
            followingList.get(follower).add(user);
            return;
        } else {
            DEBUG.messaggioDiDebug("la lista dei following non esiste");
            HashSet<String> newList = new HashSet<String>();
            DEBUG.messaggioDiDebug("creo una nuova lista");
            newList.add(follower);
            DEBUG.messaggioDiDebug("aggiungo l'utente alla lista");
            followingList.put(user, newList);
            DEBUG.messaggioDiDebug("fatto");
            return;
        }

    }

    // metodo per prendere una lista di follower di un dato utente
    public HashSet<String> getFollowers(String username) {

        return followersList.getOrDefault(username, new HashSet<String>());

    }

    // metodo per avere un set di tutti gli utenti che ne segue uno specifico
    public HashSet<String> getFollowings(String u) {
        return followingList.getOrDefault(u, new HashSet<String>());

    }

    // metodo per remuovere un follower
    public void removeFollower(String username, String follower) {
        followersList.get(username).remove(follower);
        DEBUG.messaggioDiDebug("Ho eseguito la rimozione del follower");
    }

    // rimuove un elemento dalla lista dei seguiti
    public void removeFollowing(String username, String following) {
        followingList.get(username).remove(following);
        DEBUG.messaggioDiDebug("Rimozione following completata");
    }

    // restituisce l'oggetto User corrispondente all'usernme
    public User getUser(String u) {
        return userList.getOrDefault(u, null);
    }

    // rimuove un utente da Winsome
    public void deleteUser(String u) {
        userList.remove(u);
        followersList.remove(u);
        followingList.remove(u);
    }

    // restituisce il wallet corrispondente ad un utente
    public Wallet getWallet(String username) {
        // serve anche ad inizializzarlo nel caso non esista
        walletList.putIfAbsent(username, new Wallet(username));
        return walletList.get(username);
    }

    // metodo che formatta la valuta winsome in una stringa
    public String formattedWincoin(double i) {
        String out = String.format("%." + 4 + "f", SharedMethods.approximateDouble(i));
        if (i <= 1) {
            return out + WINCOIN;
        } else {
            return out + WINCOINS;
        }
    }

    // metodo che salva l'id dell'ultimo post in maniera persistente
    public void savePostId() {
        configReader.changeSaveConfig("CurrentPostId", String.valueOf(this.currentIdPost));
    }

    // metodo che controlla la password di un utente
    public boolean checkPassword(User u, String p) throws UserNotFoundException {
        if (u == null) {
            throw new UserNotFoundException();
        } else {
            if (p.equals(u.getPassword())) {
                return true;
            } else {
                return false;
            }
        }
    }

    // metodo che ritorna quanti utenti sono iscritti a winsome
    public int getUserCounter() {
        return this.userList.size();
    }

    // metodo che aggiunge un utente
    public void addNewUser(User u) {
        userList.putIfAbsent(u.getNickname(), u);
        followersList.put(u.getNickname(), new HashSet<String>());
        followingList.put(u.getNickname(), new HashSet<String>());
    }

    // metdo per vedere se esiste un utenet
    public boolean existUser(String u) {
        return userList.containsKey(u);
    }

    /********************
     * I prossimi metodi sono usati per prendere le info dai file json
     *******/
    public void jsonPostList(ConcurrentHashMap<Integer, Post> postList) {
        this.postList = postList;
    }

    public void jsonUserList(ConcurrentHashMap<String, User> userList) {
        this.userList = userList;
    }

    public void jsonFollowerList(ConcurrentHashMap<String, HashSet<String>> followeList) {
        this.followersList = followeList;
    }

    public void jsonFollowingList(ConcurrentHashMap<String, HashSet<String>> followingList) {
        this.followingList = followingList;
    }

    public void jsonWalletList(ConcurrentHashMap<String, Wallet> walletList) {
        this.walletList = walletList;
    }

}
