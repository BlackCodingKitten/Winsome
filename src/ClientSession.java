import java.net.Socket;
/*classe che rappresenta  una sessione di un client loggato
 * l'utilità di questa classe è risalire all'utente loggato più facilmente
 */

public class ClientSession {
    private final String user;// -> user relativo alla socket
    private final Socket sessionSocket;// -> socket della sessione corrente

    // costruttore della classe ClientSession
    public ClientSession(String user, Socket sessionSocket) {
        this.user = user;
        this.sessionSocket = sessionSocket;
    }

    // getter della socket della sessione corrente
    public Socket getSocket() {
        return this.sessionSocket;
    }

    // getter dell'user relativo a questa sessione
    public String getUser() {
        return this.user;
    }

}