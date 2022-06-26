import java.io.IOException;

import Color.ColoredText;

/*questa è la classe che implementa il salvataggio dei dati nel file json
 * ogni tot tempo, specificato dal file di config del server alla voce BackupInterval
 vine inoltre eseguito un backup forzato al comando stop server perché essendo 
 uno spegnimento sicuro salva i dati prima di chiudersi*/

public class Backup implements Runnable {
    private final ConfigReader configReader;
    private final JsonFileManager fileManager;
    private final SocialManager socialManager;
    private final Rewards rewardManager;

    private volatile boolean stop = false;

    int backupInterval;

    public Backup(ConfigReader c, JsonFileManager jfm, SocialManager s, Rewards r) {
        this.fileManager = jfm;
        this.configReader = c;
        this.backupInterval = Integer.parseInt(this.configReader.getConfigValue("BackupInterval"));
        this.rewardManager = r;
        this.socialManager = s;
    }

    // metodo che cicla ogni backupInterval millisecondi per salvare i dati in
    // maniera persistente
    @Override
    public void run() {
        while (!stop) {
            // avevo messo la sleep del thread in fondo, ma poi, riflettendo ho
            // capito che all'avvio del thread faceva immediatamente un salvataggio
            try {

                Thread.sleep(this.backupInterval);
            } catch (InterruptedException e) {
                // DEBUG.messaggioDiDebug("interrupted exception nel backup manager");
                // ignored
            }
            try {
                // chiamo il metodo del JsonFilemanager che mi salva tutto insieme
                this.fileManager.save(this.socialManager);
                // salvo l'id Post contenuto nel SocialManager
                this.socialManager.saveCurrentIdPostOnConfigFile();
                // salvo la data dell'ultimo reward
                this.rewardManager.saveLastReward();
                System.out.println("Salvataggio stato interno Winsome completato.");
            } catch (IOException e) {
                System.err.println("Impossibile eseguire correttamente il salvataggio dei dati\n");
                e.printStackTrace();
            }

        }
    }


}
