import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// questa classe notifica l'aggiornamneto della lista dei follower tramite rmi sul client che poi aggiorna la sua lista di follower salvata in locale

public class NotifyEvent extends RemoteObject implements NotifyEventInterface {
    private static final String FOLLOW = "+";
    private static final String UNFOLLOW = "-";

    //private static final DEBUG debug = new DEBUG();

    @Override
    public void notifyEvent(String s) throws RemoteException {

        if (s.startsWith(UNFOLLOW)) {
            String follower = s.substring(1);
            // il nome utente parte dalla casella s[1] perchè prima c'è il simbolo + o -
            WinsomeClientMain.followerList.remove(follower);
            // debug.messaggioDiDebug(follower+" ha smesso di seguirti");
        } else {
            if (s.startsWith(FOLLOW)) {
                String follower = s.substring(1);
                WinsomeClientMain.followerList.add(follower);
                // debug.messaggioDiDebug(follower + "ha iniziato a seguirti seguirti");
            } else {
                // la stringa non comincia ne con + ne con -
                System.out.println("Operazione sconosciuta.");
            }
        }

    }

}
