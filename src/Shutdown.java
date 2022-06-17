import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import java.util.TimerTask;
import Color.ColoredText;

public class Shutdown extends TimerTask {
    @Override
    public void run() {
        InputHandler.stopWinsomeServer();
        Rewards.stopServer();
        Backup.stopServer();
         // chiusura del'rmi
         try {
            // le chiudo tutte e 2 insieme
            UnicastRemoteObject.unexportObject(WinsomeServerMain.winsomeService, false);
            UnicastRemoteObject.unexportObject(WinsomeServerMain.winsomeCallback, false);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
        System.out.println(ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE
                + "WinsomeServer terminato correttamente." + ColoredText.ANSI_RESET);
        System.exit(0);
    }
}
