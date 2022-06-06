import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCrypt;

/*Classe utente, per ogni utente conosciamo, nickname, password e lista dei tag */

public class User {
    private static final String salt = "0x0x0EsameLaboratorioReti28Giugno2022SocialWinsome0x0x0";

    private final String nickname;// ->nickname con cui si salva l'utenet
    private final Set<String> tags = new LinkedHashSet<>();// -> lista dei tag che inserisce l'utente
    private final String password;// ->password dell'utente salvata

    // costruttore della classe user
    public User(String nickname, String plaintext, List<String> tags) {
        this.nickname = nickname.toLowerCase();
        for (String t : tags) {
            t = t.toUpperCase();// voglio che i tag siano scritti in maiuscolo
            this.tags.add(t);
        }
        this.password = encryptedPassword(plaintext);

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
    public String getEncryptedPassword() {
        return this.password;
    }

    /*
     * Il metodo encryptedPassword effettua l'hashing di una stringa
     * usata anche per verificare che la password inserita sia corretta
     */
    public static String encryptedPassword(String plaintext) {
        String encrypted = null;
        encrypted = BCrypt.hashpw(plaintext, salt);
        // System.out.println("MESSAGGIO DI DEBUG: "+ plaintext + "-->"+ encrypted);
        return encrypted;
    }

    // metodo che controlla che la password inserita sia corretta
    public boolean isPasswordCorrect(String plaintext, String hashed) {
        if (hashed.equals(encryptedPassword(plaintext))) {
            return true;
        } else {
            return false;
        }
    }

}
