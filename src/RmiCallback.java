import java.rmi.RemoteException;
import java.util.HashMap;

/*e la classe che permette di registrarsi per poter ricevere notifiche di aggiornamento della lista follower */

public class RmiCallback implements RmiCallbackInterface {
    // viene mantenuta un hashmap che usa il nome utente come chiave e associa la
    // relativa interfaccia su cio inviare la notifica
    private static final HashMap<String, NotifyEventInterface> userClients = new HashMap<>();

    // permette di registrarsi al callback
    @Override
    public void callbackRegister(String nickname, NotifyEventInterface cInterface) throws RemoteException {
        NotifyEventInterface check = userClients.putIfAbsent(nickname, cInterface);
        if (check != null) {
            System.out.println("***** (TT__TT) *****\nUtente già iscritto al servizio di notifica.");
        } else {
            System.out.println("***** (^o^) *****\nIscrizione al servizio di notifica avvenuta con succcesso.");
        }
    }
    //permette di rimuoversi dalla lista di utenti che ricevono notifiche
    @Override
    public void callbackUnregister(String nickname) throws RemoteException {
        userClients.remove(nickname);
        System.out.println("***** (^///^) *****\nUtente rimosso dal servizio di notifica con successo.");
    }

    public static void followeUpdate(String username, String notif)throws RemoteException{
        if(userClients.containsKey(username)){
            userClients.get(username).notifyEvent(notif);
        }
    }

}
