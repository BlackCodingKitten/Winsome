import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
/*classe Post del social winsome  */

public class Post {
    private final String postId; // -> id univoco del post
    private final User owner; // -> autore del post
    private final String title; // -> titolo del post
    private final String text; // ->contenuto testuale del post
    private final Date date; // -> data in cui è stato scritto il post
    private final ConcurrentHashMap<User, Vote> allPostVotes;// -> tutti i voti dati ad un post
    private final ArrayList<Comment> postComment; // -> ArrayList dei commenti degli utenti al post

    private int nIterazioni; // -> variabile per il calcolo delle ricompense, viene incrementata ogni volta
                             // che il gestore premi controlla il post
    private final ConcurrentLinkedDeque<User> postRewinUser; // -> utenti che hanno fatto il "rewin"(retweet) del post
    // rewin e nIterazioni non sono inerenti alla classe post, ma per comodità sono
    // stati inseriti
    // come attributi, questi fanno si che sebbene la classe post in se sia
    // immutabile nIterazioni è modificabile (non ha la keyword final)

    // costruttore della classe post
    public Post(User owner, String title, String text, int id) {
        this.owner = owner;
        this.date = new Date();
        this.postId = owner.getNickname().toUpperCase() + String.valueOf(id);
        this.title = title;
        this.text = text;
        this.postComment = new ArrayList<>();
        this.postRewinUser = new ConcurrentLinkedDeque<>();
        this.allPostVotes = new ConcurrentHashMap<>();
        this.nIterazioni = 0;// inizializzo a zero il numero di iterazioni
    }

    // metodo getter dell'id del post
    public String getpostId() {
        return this.postId;
    }

    // metodo getter della data di pubblicazione
    public Date getDate() {
        return this.date;
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
    public User getOwner() {
        return this.owner;
    }

    // metodo getter della lista dei voti
    public ConcurrentHashMap<User, Vote> getVotes() {
        return allPostVotes;
    }

    // metodo getter della lista dei commenti
    public ArrayList<Comment> getComments() {
        return this.postComment;
    }

    // metodo per aggiungere un voto alla lista
    public void addNewVote(User user, int v) {
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
    public Integer getVoteByUser(User user) {
        if (this.allPostVotes.get(user) != null) {
            return this.allPostVotes.get(user).getVote();
        } else {
            return null;
        }
    }

}