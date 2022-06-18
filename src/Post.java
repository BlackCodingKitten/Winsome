
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*classe Post del social winsome  */
import java.util.concurrent.ConcurrentLinkedQueue;

public class Post {
    private final int postId; // -> id univoco del post
    private final String owner; // -> autore del post
    private final String title; // -> titolo del post
    private final String text; // ->contenuto testuale del post
    private final Date date; // -> data in cui è stato scritto il post
    private final ConcurrentHashMap<String, Vote> allPostVotes;// -> tutti i voti dati ad un post
    private final HashSet<Comment> postComment; // ->HashSet dei commenti degli utenti al post

    private int nIterazioni; // -> variabile per il calcolo delle ricompense, viene incrementata ogni volta
                             // che il gestore premi controlla il post
    private final ConcurrentLinkedQueue<String> postRewinUser; // -> utenti che hanno fatto il "rewin"(retweet) del post
    // rewin e nIterazioni non sono inerenti alla classe post, ma per comodità sono
    // stati inseriti
    // come attributi, questi fanno si che sebbene la classe post in se sia
    // immutabile nIterazioni è modificabile (non ha la keyword final)

    // costruttore della classe post
    public Post(String owner, String title, String text, int id) {
        this.owner = owner;
        this.date = new Date();
        this.postId = id;
        this.title = title;
        this.text = text;
        this.postComment = new HashSet<>();
        this.postRewinUser = new ConcurrentLinkedQueue<>();
        this.allPostVotes = new ConcurrentHashMap<>();
        this.nIterazioni = 0;// inizializzo a zero il numero di iterazioni
    }

    // metodo getter dell'id del post
    public int getpostId() {
        return this.postId;
    }

    // metodo getter della data di pubblicazione
    public String getDate() {
        return this.date.toString();
    }

    // metodo getter del titolo del post
    public String getTitle() {
        return this.title;
    }

    // metodo getter del testo del post
    public String getText() {
        return this.text;
    }

    // metodo getter dell'utente
    public String getOwner() {
        return this.owner;
    }

    // metodo getter della lista dei voti
    public ConcurrentHashMap<String, Vote> getVotes() {
        return allPostVotes;
    }

    //ritorna un aloista di voti dopo unacera data
    public ConcurrentHashMap<String, Vote> newPeopleLike(Date d){
        ConcurrentHashMap<String, Vote> voteList = new ConcurrentHashMap<>();
        for(Vote v : allPostVotes.values()){
            if (v.getDate().after(d)){
                voteList.put(v.getUser(), v);
            }
        }
        return voteList;
    } 

    // metodo getter della lista dei commenti
    public HashSet<Comment> getComments() {
        return this.postComment;
    }

    // metodo per aggiungere commenti alla lista commenti
    public void addNewComment(String user, String text) {
        this.postComment.add(new Comment(user, text));
    }

    // metodo per trovare tutti i commenti fatti da uno specifico utente
    public Set<Comment> getCommentByUser(String username) {
        HashSet<Comment> byUser = new HashSet<>();
        for (Comment c : this.postComment) {
            if (c.getOwner().equals(username)) {
                byUser.add(c);
            }

        }
        return byUser;
    }

    // metodo che ritorna la lista di tutti gli utenti che hanno commentato il post
    // dopo una certa data
    public Set<String> getListUserCommentingAfterDate(Date afterDate) {
        HashSet<String> userCommentingList = new HashSet<>();
        for (Comment c : this.postComment) {
            // se l'user non è già stato messo in lista e il commento è stato pubblicato
            // dopo una certa data
            if (!userCommentingList.contains(c.getOwner()) && c.getDate().after(afterDate)) {
                userCommentingList.add(c.getOwner());
            }
        }
        return userCommentingList;
    }

    // metodo per aggiungere un voto alla lista
    public void addNewVote(String user, int v) {
        allPostVotes.putIfAbsent(user, new Vote(user, v));
    }

    // metodo per calcolare il totale dei voti
    public int getNumVotes() {
        return allPostVotes.size();
    }

    // metodo per calcolare gli uppervote
    public int getNumUpperVotes() {
        int num = 0;
        for (Vote val : allPostVotes.values()) {
            if (val.getVote() == 1) {
                num++;
            }
        }
        return num;
    }

    // metodo per calcolare il numero di downvote
    public int getNumDownVotes() {
        return getNumVotes() - getNumUpperVotes();
    }

    // metodo per trovare un voto nella lista dei voti al post per nome utente
    public Integer getVoteByUser(String user) {
        if (this.allPostVotes.get(user) != null) {
            return this.allPostVotes.get(user).getVote();
        } else {
            return null;
        }
    }

    // metodo getter della lista degli utenti che hanno fatto il rewin del post
    public ConcurrentLinkedQueue<String> getRewinUsers() {
        return this.postRewinUser;
    }

    // metodo per controllare se un utente ha già fatto il rewin
    public boolean isNotUserRewinedPost(String user) {
        if (this.postRewinUser.contains(user)) {
            return false;// -> ritorna false se l'utente è già in lista
        }
        return true;// -> l'utente non ha mai fatto il rewind del post
    }

    // metodo che aggiunge un utente alla lista rewind è boolean perchè serve a
    // sollevare un'eccezione in caso l'utente
    // sia già presente
    public boolean addRewineUser(String user) {
        if (isNotUserRewinedPost(user)) {
            this.postRewinUser.add(user);
            return true; // utente inserito correttemente in lista
        }
        return false;// utente già inserito impossibile inserirlo nuovamente
    }

    // metodo getter del numero di iterazioni
    public int getIteration() {
        return this.nIterazioni;
    }

    // metodo di incremento del numero di iterazioni
    public void addIteration() {
        this.nIterazioni++;
    }

    @Override
    public int hashCode(){
        return String.valueOf(this.postId).hashCode();
    }
    @Override
    public boolean equals(Object p){
        return this.postId == ((Post)p).getpostId();
    }
}