import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

import Color.ColoredText;

// questa classe notifica l'aggiornamneto della lista dei follower tramite rmi sul client che poi aggiorna la sua lista di follower salvata in locale

public class NotifyEvent extends RemoteObject implements NotifyEventInterface {
    private static final String FOLLOW = "+";
    private static final String UNFOLLOW = "-";

    // private static final DEBUG debug = new DEBUG();

    @Override
    public void notifyEvent(String s) throws RemoteException {

        if (s.startsWith(UNFOLLOW)) {
            String follower = s.substring(1);
            // il nome utente parte dalla casella s[1] perchè prima c'è il simbolo + o -
            WinsomeClientMain.followerList.remove(follower);
            System.out.println("\n" + ColoredText.ANSI_BLUE + ColoredText.ANSI_BOLD + "NUOVA NOTIFICA:\t"
                    + ColoredText.ANSI_RESET + ColoredText.ANSI_WHITE_BACKGROUND
                    + ColoredText.ANSI_PURPLE
                    + follower + ColoredText.ANSI_RESET + ColoredText.ANSI_BLUE + " ha smesso di seguirti."
                    + ColoredText.ANSI_RESET);
        } else {
            if (s.startsWith(FOLLOW)) {
                String follower = s.substring(1);
                WinsomeClientMain.followerList.add(follower);
                System.out.println("\n" + ColoredText.ANSI_BLUE + ColoredText.ANSI_BOLD + "NUOVA NOTIFICA:\t"
                        + ColoredText.ANSI_RESET
                        + ColoredText.ANSI_WHITE_BACKGROUND + ColoredText.ANSI_PURPLE + follower
                        + ColoredText.ANSI_RESET
                        + ColoredText.ANSI_BLUE + " ha iniziato a seguirti." + ColoredText.ANSI_RESET);
            } else {
                // la stringa non comincia ne con + ne con -
                System.out.println("Operazione sconosciuta.");
            }
        }

    }

}
