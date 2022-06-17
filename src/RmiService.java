import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

/*Qusta classe è quella che permette al client di 
registrarsi via Rmi a Winsome
cioè tutti i metdi implementati in questa classse sono chiamati dal client ma implementati lato server */

public class RmiService implements RmiServiceInterface {

    // metodo che permette la register di un utente winsome ritorna true in caso di
    // registrazione avvenuta con successo,
    // false altrimenti, i motivi di fallimento della registrazione possono essere 2
    // username già utilizzato, il cui controllo
    // è affidato al social manager che ricerca l'utente nella lista via nickname, o
    // lista dei tag troppo lunga
    // (possono essere ,max 5 );
    @Override
    public boolean registerNewUser(String nickname, String password, HashSet<String> tags) throws RemoteException {
        nickname = nickname.toLowerCase();
        System.out.println("Inizio fase di registrazione dell'utente " + nickname);
        if (tags.size() > 5 || tags.size() == 0) {
            System.err.println("Errore nella lista tag.");
            return false;
        }
        if (!usernameNeverUsed(nickname)) {
            System.err.println("Username già registrato.");
            return false;
        }
        SocialManager socialManager = WinsomeServerMain.socialManager;
        User u = new User(nickname, password, tags);
        socialManager.addNewUser(u);
        System.out.println("Fatto!");
        return true;

    }

    // questo metodo fornice la lista dei followers di un utente, se non presente
    // restituisce la lista vuota
    @Override
    public Set<String> followerList(String nickname) throws RemoteException {
        SocialManager socialManager = WinsomeServerMain.socialManager;
        User user = socialManager.getUser(nickname);
        if (user != null) {

        Set<String> followerList = socialManager.getFollowers(nickname);
        return followerList;

        }
        return new HashSet<>();// restituisco una lista vuota
    }

    // controlla se il nickname inserito non appartiene ad un altro utente.
    private boolean usernameNeverUsed(String nickname) {
        SocialManager socialManager = WinsomeServerMain.socialManager;
        // il metodo exist User ritorna true se il nome utente è stato usato, false
        // altrimenti
        return !socialManager.existUser(nickname);
    }

}
