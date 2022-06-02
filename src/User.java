import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.*;
import org.springframework.security.crypto.bcrypt.BCrypt;

/*Classe utente, per ogni utente conosciamo, nickname, password e lista dei tag */

public class User {

    private final String nickname;
    private final Set<String> tags = new LinkedHashSet<>();
    private final String password;

    //costruttore della classe user
    public User(String nickname, String plaintext, List<String> tags) {
        this.nickname=nickname;
        for(String t : tags){
            this.tags.add(t);
        }
        this.password = encryptedPassword(plaintext);

    }

    public String getNickname(){
        return this.nickname;
    }

    public Set<String> getTags (){
        return this.tags;
    }

    /*la funzione encripted password effettua l'hashing di una stringa
    usata anche per verificare che la password inserita sia corretta*/
    public static String encryptedPassword(String plaintext){
        String encrypted = null;
        encrypted=BCrypt.hashpw(plaintext, BCrypt.gensalt());
        //Systrem.out.println("MESSAGGIO DI DEBUG: "+ plaintext + "-->"+ encrypted);
        return encrypted;
    }

}
