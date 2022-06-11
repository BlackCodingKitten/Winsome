import java.util.Date;

/*questa classe rappresenta i commenti che possono essere fatti sotto ai post
contine autore del commemto, la data in cui è stato inviato e il testo del commento*/

public class Comment {
    private final String  owner;// ->autore del commento
    private final String text;// ->testo del commento
    private final Date date;// -> data di pubblicazione del commento

    // costruttore della classe Comment, il metodo Date() usa data e ora correnti
    // per inizializzare un oggetto
    public Comment(String owner, String text) {
        this.text = text;
        this.owner = owner;
        this.date = new Date();
    }

    // metodo getter per leggere il testo del post
    public String getText() {
        return this.text;
    }

    // metodo getter per vedere l'autore del post
    public String getOwner() {
        return this.owner;
    }

    // metodo getter per la data di pubblicazione del commento
    public Date getDate() {
        return this.date;
    }


    @Override
    public int hashCode(){
        return this.text.hashCode();
    }

}