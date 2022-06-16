import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import Color.ColoredText;

/*l'input handler si occupa della gestione delle richieste del server,
 è una classe ad esclusivo utilizzo per la parte amministrativa*/
public class InputHandler implements Runnable {
    private final SocialManager socialManager;
    private final ThreadPoolExecutor pool;

    private volatile static boolean stop = false;

    private String defaultReason = "Nessuna causale inserita.";

    public InputHandler(SocialManager socialManager, ThreadPoolExecutor pool) {
        this.pool = pool;
        this.socialManager = socialManager;
    }

    @Override
    // la run viene eseguita all'avvio del server e aspetta i comandi dal client
    public void run() {
        Scanner input = new Scanner(System.in);
        String adminRequest = null;

        while (!stop) {
            adminRequest = SharedMethods.readFromConsole(input);
            if (adminRequest.equalsIgnoreCase("")) {
                // se la richiesta del client è vuota mi rimetto in attesa
                continue;
            } else {
                // faccio lo split dell'input utente diviso in comando e argomenti
                String[] splittedInput = adminRequest.split(" ");
                // prendo il comando da eseguire
                String command = splittedInput[0];// il comando è la prima parola
                String[] args = new String[splittedInput.length - 1];
                System.arraycopy(splittedInput, 1, args, 0, splittedInput.length - 1);

                switch (command) {
                    case "stopserver": // ferma il server in maniera sicura salvando i dati
                        // DEBUG.messaggioDiDebug("Spegnimento server in maniera sicura");
                        stopWinsomeServer();
                        break;
                    case "listusers":
                        // mostra un alista di tutti gli utenti iscritti a winsome
                        ListUsers();
                        break;
                    case "listfollowers":
                        // mostra una lista dei followers di un determinato utente
                        if (args.length != 1) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        listFollower(args[0]);
                        break;
                    case "listfollowing":
                        // mostra una lista dei seguiti di un determinato utente
                        if (args.length != 1) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        listFollowing(args[0]);
                        break;
                    case "addfollower":
                        // aggiunge un follower ad un utente
                        if (args.length != 2) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        addFollower(args[0], args[1]);
                        break;
                    case "removefollower":
                        // rimuove un follower da un utente
                        if (args.length != 2) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        removeFollower(args[0], args[1]);
                        break;
                    case "addfollowing":
                        // aggiunge un utente alla lista seguiti
                        if (args.length != 2) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        addFollowing(args[0], args[1]);
                        break;
                    case "removefollowing":
                        // rimuove un seguito da un utente
                        if (args.length != 2) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        removeFollowing(args[0], args[1]);
                        break;
                    case "winsome":
                        // stampa alcune statistiche del server, come ad esempio l'utente più ricco
                        winsome();
                        break;
                    case "help":
                        // stampa una lista dei comandi sopraelencati
                        help();
                        break;
                    case "transaction":
                        // avvia una nuova trasazione
                        if (args.length != 2) {
                            System.out.println("Comando errato, per sapere come eseguire il comando digita \"help\".");
                            break;
                        }
                        transaction(args[0], Double.parseDouble(args[1]));
                        break;
                    default:
                        // comando non riconosciuto
                        System.out.println(
                                "Comado inesistente, per favore digita un comando valido.\nPer conoscere quali sono i comandi validi digita \"help\"");
                }
            }
        }
    }

    private void ListUsers() {
        int totUser = this.socialManager.getUserCounter();
        if (totUser == 0) {
            System.out.println("Ancora nessun utente registrato al social Winsome.");
        } else {
            System.out.println(ColoredText.ANSI_PURPLE + "Lista degli utenti registrati" + ColoredText.ANSI_RESET);
            int i = 1;
            for (User user : socialManager.getUserList().values()) {
                System.out.print(i + ") " + user.getNickname() + "\n");
                i++;
            }
        }
    }

    private void listFollower(String u) {
        if (!socialManager.existUser(u)) {
            System.out.print("Utente inesistente.");
            return;
        }
        Set<String> followersList = new HashSet<>();
        followersList = socialManager.getFollowers(u);
        if (followersList == null) {
            System.out.println("L'utente " + u + " non ha nessun follower.");
            return;
        }
        System.out.println(ColoredText.ANSI_PURPLE + "Lista utenti follower di " + ColoredText.ANSI_WHITE_BACKGROUND + u
                + ColoredText.ANSI_RESET + "\n");
        int i = 1;
        for (String us : followersList) {
            System.out.println(i + ") " + us);
            i++;
        }

    }

    private void listFollowing(String u) {
        if (!socialManager.existUser(u)) {
            System.out.print("Utente inesistente.");
            return;
        }
        Set<String> followingList = socialManager.getFollowings(u);
        if (followingList == null) {
            System.out.println("L'utente " + u + " non segue nessuno.");
        } else {
            System.out
                    .println(ColoredText.ANSI_PURPLE + "Lista utenti che segue " + ColoredText.ANSI_WHITE_BACKGROUND + u
                            + ColoredText.ANSI_RESET + "\n");
            int i = 1;
            for (String user : followingList) {
                System.out.println(i + ") " + user);
                i++;
            }
        }
    }

    private void addFollower(String u, String nf) {
        try {
            RmiCallback.followeUpdate(u, "+" + nf);
            System.out.println(u + " ha un nuovo follower: " + ColoredText.ANSI_WHITE_BACKGROUND
                    + ColoredText.ANSI_PURPLE + nf + ColoredText.ANSI_RESET);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void removeFollower(String u, String nf) {
        try {
            RmiCallback.followeUpdate(u, "-" + nf);
            System.out.println(ColoredText.ANSI_WHITE_BACKGROUND + ColoredText.ANSI_PURPLE + nf + ColoredText.ANSI_RESET
                    + " ha smesso di seguire " + u);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void addFollowing(String u, String nfw) {
        socialManager.addNewFollower(nfw, u);
        socialManager.addNewFollowing(u, nfw);
        System.out.println("L'utente " + u + " segue " + nfw);
    }

    private void removeFollowing(String u, String rf) {
        socialManager.removeFollower(rf, u);
        socialManager.removeFollowing(u, rf);
        System.out.println("l'utente " + u + "non segue più l'utente " + rf);
    }

    private void transaction(String u, double amount) {
        if (socialManager.getUser(u) == null) {
            System.out.println("L'utente " + u + " non esiste");
            return;
        } else {
            double newBalance = socialManager.getWallet(u).amountUpdate(defaultReason, amount);
            System.out.print("Il portafoglio di " + u + " contiene " + ColoredText.ANSI_PURPLE_BACKGROUND + newBalance);
            if (newBalance > 1) {
                System.out.print(" Wincoins" + ColoredText.ANSI_RESET);
            } else {
                System.out.print(" Wincoin" + ColoredText.ANSI_RESET);
            }
        }
    }

    public static void stopWinsomeServer() {
        System.out.println("WinsomeServer è in arresto...\nSperiamo che nessun orco lo rapisca nel sonno (-o-)\n");
        stop = true;
        try {
            WinsomeServerMain.serverSocket.close();
        } catch (IOException e) {
            /* ignored */
        }
    }

    private void winsome() {
        Runtime winsomeInstance = Runtime.getRuntime();// ->Restituisce l'oggetto runtime associato all'applicazione
                                                       // Java corrente.
        System.out.println(ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE + "**************"
                + ColoredText.ANSI_RESET + ColoredText.ANSI_WHITE_BACKGROUND + ColoredText.ANSI_PURPLE
                + " WinsomeStats " + ColoredText.ANSI_RESET + ColoredText.ANSI_PURPLE_BACKGROUND
                + ColoredText.ANSI_WHITE + "**************" + ColoredText.ANSI_RESET);
        System.out.println("Utenti totali registrati: " + socialManager.getUserCounter());
        System.out.println("Socket attualmente in uso: " + WinsomeServerMain.clientSocketList.size());
        System.out.println("Numero di thread attivi attualmente nel ThreadPool: " + pool.getActiveCount());
        System.out.println("Numero di thread totali nel Threadpool: " + pool.getPoolSize());
        System.out.println("Numero massimo di thread attivati contemporaneamente: " + pool.getLargestPoolSize());
        // stampa anche l'utilizzo di memoria a runtime in bytes grazie alla classe
        // Runtime
        // maxMemory() Restituisce la quantità massima di memoria che la macchina
        // virtuale Java tenterà di utilizzare.
        // totalMeomory() Restituisce la quantità totale di memoria nella macchina
        // virtuale Java. Il valore restituito da questo metodo può variare nel tempo
        // freeMemory() Restituisce la quantità di memoria libera nella Java Virtual
        // Machine.
        System.out.println("\n" + (winsomeInstance.totalMemory() - winsomeInstance.freeMemory()) / (1024 * 1024)
                + " bytes in uso di " + (winsomeInstance.totalMemory() / (1024 * 1024)) + " bytes totali su "
                + winsomeInstance.maxMemory() / (1024 * 1024) + " bytes della JVM.\n");
        System.out.println(ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE
                + "******************************************" + ColoredText.ANSI_RESET);
    }

    private void help() {
        System.out.println("LISTA COMANDI:\n");
        System.out.println("stopserver  Termina il server in maniera sicura.");
        System.out.println("listusers   Stampa la lista degli utenti attuamente registrati.");
        System.out.println("listfollowers <username>   Stampa la lista di tutti i seguaci dell'utente specificato.");
        System.out.println("listfollowing <useranme>   Stampa la lista di tutti i seguiti da un'utente.");
        System.out.println("addfollower <username> <follower>  Follower inizia a seguire Username.");
        System.out.println("addfollowing <username> <following> Username inizia a seguire Following. ");
        System.out.println(
                "removefollower <username> <follower>  Rimuove l'utente \"follower\" dalla lista follower di username.");
        System.out.println("winsome   mostra le statistiche in tempo reale del server Winsome.");
        System.out.println(
                "removefollowing <utente> <following> Rimuove \"following\" dalla lista dei seguiti di username. ");
        System.out.println("transaction  <username> <valore>   Effettua una transazione in favore di username ");
        System.out.println("help  Stampa questa lista");

    }

}
