import java.util.Set;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.spi.DecimalFormatSymbolsProvider;
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

        if (username.equalsIgnoreCase(following)) {
            throw new InvalidOperationException();
        }

        if (!userList.containsKey(following)) {
            throw new UserNotFoundException();
        }

        if (!followersList.getOrDefault(following, new HashSet<String>()).contains(username)) {
            throw new InvalidOperationException();
        }

        removeFollower(following, username);
        removeFollowing(username, following);

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

            if (!post.getOwner().equalsIgnoreCase(username)) {
                // se l'utente che vuole eliminare il post non è l'autore
                throw new InvalidOperationException();
            }
        }

        postList.remove(id);
    }

    // metodo per recuperare il feed di uno specifico utente
    public HashSet<Post> getUserFeed(String username) {

        HashSet<Post> postInFeed = new HashSet<Post>();
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
            if (p.getOwner().equalsIgnoreCase(username) || !p.isNotUserRewinedPost(username)) {
                blog.add(p);
            }
        }
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
            // post non trovato
            throw new PostNotFoundException();
        } else {
            if (post.getOwner().equalsIgnoreCase(username)) {
                // l'autore del post può commentarlo
                post.addNewComment(username, comment);
                return;
            }
            if (!isPostInFeed(id, username)) {
                // post non in feed
                throw new PostNotInFeedException();
            } else {
                post.addNewComment(username, comment);
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
        stringBuilder.append(String.valueOf(ColoredText.ANSI_PURPLE + commentList.size()) + ColoredText.ANSI_RESET);
        if (commentList.size() == 1) {
            stringBuilder.append(" utente ha commentato questo post.");
        } else {
            stringBuilder.append(" utenti hanno commentato questo post.");
        }
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

        Post post = postList.getOrDefault(id, null);
        if (post == null) {
            // il post non esiste
            throw new PostNotFoundException();
        }
        if (post.getOwner().equalsIgnoreCase(username)) {
            throw new SameUserException();
        }
        if (!isPostInFeed(id, username)) {
            throw new PostNotInFeedException();
        }
        boolean flag = post.addRewineUser(username);// metodo eseguito dalla classe post
        if (!flag) {
            // se ritorno false vuol dire che ho già fatto la rewin al post
            throw new InvalidOperationException();
        }

    }

    // metoodo per seguire un'altro utente
    public void follow(String follower, String user)
            throws UserNotFoundException, InvalidOperationException, SameUserException {

        if (follower.equalsIgnoreCase(user)) {
            // l'utente st cercando si seguirsi da solo
            throw new SameUserException();
        }

        if (!userList.containsKey(user)) {
            // user non è registrato
            throw new UserNotFoundException();
        }

        if (followingList.getOrDefault(follower, new HashSet<String>()).contains(user)) {
            // segui già l'utente user
            throw new InvalidOperationException();
        }
        // aggiorno la lista follower
        addNewFollower(user, follower);
        // aggiorno la lista following
        addNewFollowing(follower, user);
    }

    // metodo per aggiungere un utente alla follower list
    public void addNewFollower(String user, String follower) {

        if (followersList.containsKey(user)) {
            // se già esiste una followers list
            followersList.get(user).add(follower);

        } else {
            // se non este una follower list
            HashSet<String> newList = new HashSet<>();
            newList.add(follower);
            followersList.put(user, newList);

        }

    }

    // metodo che aggiorna la lista following
    public void addNewFollowing(String follower, String user) {
        if (followingList.containsKey(follower)) {
            // se esiste già una following list
            followingList.get(follower).add(user);
            return;
        } else {
            // se non esisteva già una following list
            HashSet<String> newList = new HashSet<String>();
            newList.add(follower);
            followingList.put(user, newList);
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
    }

    // rimuove un elemento dalla lista dei seguiti
    public void removeFollowing(String username, String following) {
        followingList.get(username).remove(following);
    }

    // restituisce l'oggetto User corrispondente all'usernme
    public User getUser(String u) {
        return userList.getOrDefault(u, null);
    }

    // restituisce il wallet corrispondente ad un utente
    public Wallet getWallet(String username) {
        // serve anche ad inizializzarlo nel caso non esista
        walletList.putIfAbsent(username, new Wallet(username));
        return walletList.get(username);
    }

    // metodo che formatta la valuta winsome in una stringa
    public String formattedWincoin(double i) {
        DecimalFormat df = new DecimalFormat("0.0000");
        String out = df.format(i);
        if (i <= 1) {
            //nome valuta singolare 
            return out + " " + WINCOIN;
        } else {
            //nome valuta plurale
            return out + " " + WINCOINS;
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
                //password corretta
                return true;
            } else {
                //password sbagliata
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
        //inizializzo anche followerList e followingList
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
