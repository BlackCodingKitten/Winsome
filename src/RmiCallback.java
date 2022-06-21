import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

/*e la classe che permette di registrarsi per poter ricevere notifiche di aggiornamento della lista follower */

public class RmiCallback implements RmiCallbackInterface {
    // viene mantenuta un hashmap che usa il nome utente come chiave e associa la
    // relativa interfaccia su cio inviare la notifica
    private static final ConcurrentHashMap<String, NotifyEventInterface> userClients = new ConcurrentHashMap<>();

    // permette di registrarsi al callback
    @Override
    public synchronized void callbackRegister(String nickname, NotifyEventInterface cInterface) throws RemoteException {
        NotifyEventInterface check = userClients.putIfAbsent(nickname, cInterface);
        if (check != null) {
            System.out.println(nickname + " e' gia' loggato.");
        } else {
            System.out.println(nickname + " login.");
        }
    }

    // permette di rimuoversi dalla lista di utenti che ricevono notifiche
    @Override
    public synchronized void callbackUnregister(String nickname) throws RemoteException {
        userClients.remove(nickname);
        System.out.println(nickname + "  logout.");
    }

    public static void followeUpdate(String username, String notif) throws RemoteException {
        if (userClients.containsKey(username)) {
            userClients.get(username).notifyEvent(notif);
        }
    }

}
