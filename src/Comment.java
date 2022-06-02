import java.util.Date;
import java.time.LocalDateTime;

/*questa classe rappresenta i commenti che possono essere fatti sotto ai post
contine autore del commemto, la data in cui è stato inviato e il testo del commento*/

public class Comment {
    private final String owner;// ->autore del commento
    private final String text;// ->testo del commento
    private final Date date;// -> data di pubblicazione del commento

    // costruttore della classe Comment, il metodo Date() usa data e ora correnti
    // per inizializzare un oggetto
    public Comment(String owner, String text) {
        this.text = text;
        this.owner = owner;
        this.date = new Date();
    }

    // metodo per leggere il testo del post
    public String getText() {
        return this.text;
    }

    // metodo per vedere l'autore del post
    public String getOwner() {
        return this.owner;
    }

    // metodo per avre la data di pubblicazione del commento
    public String getDate() {
        return this.date;
    }

}