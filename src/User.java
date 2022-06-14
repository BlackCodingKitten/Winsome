import java.util.LinkedHashSet;
import java.util.Set;

/*Classe utente, per ogni utente conosciamo, nickname, password e lista dei tag */

public class User {
    // private static final DEBUG debug = new DEBUG();

    private final String nickname;// ->nickname con cui si salva l'utenet
    private final Set<String> tags = new LinkedHashSet<>();// -> lista dei tag che inserisce l'utente
    private final String password;// ->password dell'utente salvata

    // costruttore della classe user
    public User(String nickname, String plaintext, Set<String> tags) {
        this.nickname = nickname.toLowerCase();
        for (String t : tags) {
            t = t.toUpperCase();// voglio che i tag siano scritti in maiuscolo
            // debug.messaggioDiDebug(t);
            this.tags.add(t);
        }
        this.password = plaintext;

    }

    // ritorna il nome utente
    public String getNickname() {
        return this.nickname;
    }

    // ritorna la lista dei tag
    public Set<String> getTags() {
        return this.tags;
    }

    // ritorna la password dell'utente
    public String getPassword() {
        return this.password;
    }

    @Override
    public int hashCode() {
        return this.nickname.hashCode();
    }

    @Override
    public boolean equals(Object u) {
        return this.nickname.equalsIgnoreCase(((User) u).getNickname());
    }

}
