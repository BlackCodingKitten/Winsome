import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

//NioMultiplexingServer
public class WinsomeServerMain implements RmiWinsomeServerInterface {

    private List<User> Users;
    private final ObjectMapper mapper;
    private File userFile;

    public static void main(String[] args) throws IOException {

    }

    public String register(String username, String password, String[] tags) throws RemoteException {
        if (username.isEmpty() || password.isEmpty()) {
            return "Registrazione invalida - utente non registrato";
        }
        for (User user : Users) {
            if (user.getNickname().equalsIgnoreCase(username)) {
                return "Attenzione!!! Utente: " + username + " già registrato";
            }
        }
        User utente = new User(username, password, tags);
        Users.add(utente);
        try {
            mapper.writeValue(userFile, Users);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Registrazione nuovo utente avvenuta con successo";

    }

}