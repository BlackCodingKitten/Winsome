import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.HashSet;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.regex.Pattern;

import Color.ColoredText;
import CustomizedException.InvalidOperationException;
import CustomizedException.InvalidVoteValueException;
import CustomizedException.PostLengthException;
import CustomizedException.PostNotFoundException;
import CustomizedException.PostNotInFeedException;
import CustomizedException.SameUserException;
import CustomizedException.UserNotFoundException;

/*gestisce la connessione con i client, prende una client socket , ascolta le chieste su quella socket , genera le risposte */
//tutti i metodi di questa classe si riferiscono direttamente all'utente connesso alla client session corrispondente.
public class ConnectionHandler implements Runnable {
    // private final ConfigReader configReader;
    private final Socket clientSocket;
    private ClientSession clientSession;
    // private final int key;
    private final SocialManager socialManager;
    private PrintWriter output;
    private BufferedReader input;
    private boolean logoutFlag;

    private void setFlag(boolean set) {
        logoutFlag = set;
    }

    // Costruttore della classe connection handler
    public ConnectionHandler(Socket c, ConfigReader co, SocialManager s) {
        this.clientSession = null;
        // this.configReader = co;
        this.socialManager = s;
        this.clientSocket = c;
        this.output = null;
        this.input = null;
        logoutFlag = false;
    }

    // comando sconosciuto
    private void unknownCmd() {
        SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE + "Comando Sconosciuto." + ColoredText.ANSI_RESET);
    }

    // metodo di login: se utente e password sono corretti e non esiste già una
    // sessione associata a quei dati, genero una nuova sessione
    private void login(String nickname, String password) {
        if (clientSession != null && logoutFlag) {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Login gia' effettuato per questo account. Sara' il tuo gemello cattivo?"
                            + ColoredText.ANSI_RESET);
            return;
        }
        nickname = nickname.toLowerCase();
        User user = socialManager.getUser(nickname);
        if (user != null && SharedMethods.isPasswordCorrect(password, user.getPassword())) {
            // controllo che nome utente e password siano corretti
            ClientSession thisClientSession = WinsomeServerMain.clientSessionList.get(nickname);
            if (thisClientSession != null && thisClientSession.getSocket() == clientSocket) {
                // sessione già esistente per questo client
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE +
                        "Login gia' effettuato" + thisClientSession.getUser()
                        + "Sara' il tuo gemello cattivo?" + ColoredText.ANSI_RESET);
                return;
            }
            // se thisClientSession = null
            thisClientSession = new ClientSession(nickname, clientSocket);
            WinsomeServerMain.clientSessionList.put(nickname, thisClientSession);
            clientSession = thisClientSession;
            setFlag(true);
            SharedMethods.sendToStream(output, "OK");
        } else {
            // nome utente password non corretti
            SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                    + "Errore!!\nNome utente o password incorretti, riprova." + ColoredText.ANSI_RESET);
        }

    }

    // metodo che mostra il feed di un utente
    private void feed() {
        if (clientSession == null) {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Effettua prima il login, grazie." + ColoredText.ANSI_RESET);
        } else {
            String thisUser = clientSession.getUser();
            HashSet<Post> feed = socialManager.getUserFeed(thisUser);
            if (feed.size() == 0) {
                // se il feed è vuoto
                String toSend = ColoredText.ANSI_PURPLE
                        + "Il tuo feed è vuoto.\nSei un nuovo utente?, digita \"help\"." + ColoredText.ANSI_RESET;
                SharedMethods.sendToStream(output, toSend);
                return;
            }
            // se il feed ha almeno 1 post
            StringBuilder toSend = new StringBuilder();
            toSend.append("\n" + ColoredText.ANSI_PURPLE + "******FEED DI "
                    + thisUser.toUpperCase() + ColoredText.ANSI_RESET + "\n");
            for (Post pt : feed) {
                toSend.append(socialManager.formattedPost(pt.getpostId()));
            }
            SharedMethods.sendToStream(output, toSend.toString());
        }
    }

    // operazione che mostra una lista di utenti con tag in comune ad un utente
    // specifico
    private void listUserCommonTags() {
        if (clientSession == null) {
            // l'utente non ha efffettuato il login
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Login non effettuato." + ColoredText.ANSI_RESET);
        } else {
            String thisUser = clientSession.getUser();
            HashSet<String> thisUserTags = (HashSet<String>) socialManager.getUser(thisUser).getTags();
            // inutile il controllo se
            // l'utente ha almeno un tag, ottengo tutti quelli con almemo un tag uguale
            HashSet<User> usersCommonTagList = (HashSet<User>) socialManager.getCommonTagsUsers(thisUserTags);
            // rimuovo l'utente stesso dalla lista
            usersCommonTagList.remove(socialManager.getUser(thisUser));
            if (usersCommonTagList.size() == 0) {
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "Nessun utente ha tag in comune con te. (TT.TT)" + ColoredText.ANSI_RESET);
                return;
            }
            StringBuilder toSend = new StringBuilder();
            toSend.append("\n" + ColoredText.ANSI_WHITE + ColoredText.ANSI_PURPLE
                    + "LISTA DI UTENTI CHE HANNO TAG IN COMUNE CON TE:" + ColoredText.ANSI_RESET + "\n");
            for (User user : usersCommonTagList) {
                toSend.append(ColoredText.ANSI_PURPLE + user.getNickname() + " " + ColoredText.ANSI_RESET + "\t[ ");
                for (String tag : user.getTags()) {
                    // append()dei tag in comune
                    if (thisUserTags.contains(tag)) {
                        toSend.append(tag + " ");
                    }
                }
                toSend.append("]\n");
            }

            SharedMethods.sendToStream(output, toSend.toString());
        }

    }

    // l'utente invia un post con titolo e corpo
    private void post(String[] postArgument) {
        // post argument è composto da 4 stringhe:
        // pA[0]=[post ]
        // pA[1]= contiene il titolo
        // pA[2]= è uno spazio vuoto
        // pa[3]= contenuto del post
        if (clientSession == null) {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Effettua il login prima di inviare un post." + ColoredText.ANSI_RESET);
        } else {
            String thisUser = clientSession.getUser();
            String postTitle = postArgument[1];
            String post = postArgument[3];

            try {
                int idPost = socialManager.createNewPost(thisUser, postTitle, post);
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post " + ColoredText.ANSI_WHITE_BACKGROUND + idPost
                                + ColoredText.ANSI_RESET + ColoredText.ANSI_PURPLE + " pubblicato correttamente"
                                + ColoredText.ANSI_RESET);
            } catch (PostLengthException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE
                                + "Superato il limte di caratteri, inserisci un titolo o un testo più corti."
                                + ColoredText.ANSI_RESET);
            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Utente insesistente." + ColoredText.ANSI_RESET);
            }
        }
    }

    // metodo per votare un post
    private void ratePost(int id, int vote) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                    + "Prima di poter votare devi effetttuare il login." + ColoredText.ANSI_RESET);
        } else {
            try {
                String thisUser = clientSession.getUser();
                socialManager.ratePost(thisUser, id, vote);
                if (vote > 0) {
                    SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE + "Hai votato +" + vote + " il post "
                            + id + "." + ColoredText.ANSI_RESET);
                } else {
                    SharedMethods.sendToStream(output,
                            ColoredText.ANSI_PURPLE + "Hai votato " + formattedVote(vote) + " il post " + id + "."
                                    + "\nCommenta per dire cosa non ti è piaciuto." + ColoredText.ANSI_RESET);
                }

            } catch (InvalidVoteValueException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Voto non valido." + ColoredText.ANSI_RESET);
            } catch (UserNotFoundException e) {
                // errore del server
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Errore interno, per favore riprova." + ColoredText.ANSI_RESET);
                e.printStackTrace();
            } catch (PostNotFoundException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Il post che vuoi votare non esiste." + ColoredText.ANSI_RESET);
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Questo post non è nel tuo feed." + ColoredText.ANSI_RESET);
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Hai già votato questo post." + ColoredText.ANSI_RESET);
            } catch (SameUserException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Non puoi votare il tuo stesso post." + ColoredText.ANSI_RESET);
            }
        }
    }

    // stampa +1 o -1 a seconda del voto
    private String formattedVote(int v) {
        if (v == 1) {
            return "+1";
        }
        return "-1";
    }

    // metodo per commentare un post
    private void comment(int id, String comment) {
        if (clientSession == null) {
            // se il client non è loggato
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Effettua prima il login." + ColoredText.ANSI_RESET);
        } else {
            try {
                socialManager.commentPost(clientSession.getUser(), id, comment);
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Commento pubblicato correttamente." + ColoredText.ANSI_RESET);
            } catch (PostNotFoundException e) {
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "Impossibile pubblicare il commento, post inesistente." + ColoredText.ANSI_RESET);
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Impossibile pubblicare il commento, post non presente nel tuo feed."
                                + ColoredText.ANSI_RESET);
            }
        }
    }

    // metodo per cancellare un post
    private void deletePost(int id) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                    + "Effettua il login prima di cancellare un post." + ColoredText.ANSI_RESET);
        } else {
            try {
                String thisUser = clientSession.getUser();
                socialManager.deletePost(id, thisUser);
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post eliminato correttamente.\nAvevi scritto cose compromettenti?."
                                + ColoredText.ANSI_RESET);
            } catch (PostNotFoundException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post da eliminare non trovato." + ColoredText.ANSI_RESET);
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "Puoi elimnare solo i post che hai scritto te." + ColoredText.ANSI_RESET);
            }
        }
    }

    // stampa invia la lista dei segiti dell'utente o un messaggio che dice lista
    // vuota se non ne segue nessuno
    private void getFollowingsList() {
        StringBuilder toSend = new StringBuilder();
        if (clientSession == null) {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Effettua il login prima." + ColoredText.ANSI_RESET);
            return;
        } else {
            String thisUser = clientSession.getUser();
            HashSet<String> followings = (HashSet<String>) socialManager.getFollowings(thisUser);
            if (followings.size() == 0) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Non segui nessun utente." + ColoredText.ANSI_RESET);
                return;
            } else {
                toSend.append("\n" + ColoredText.ANSI_PURPLE + "LISTA DEI SEGUITI:\n" + ColoredText.ANSI_RESET);
                int i = 1;
                for (String user : followings) {
                    toSend.append(ColoredText.ANSI_PURPLE + i + ")" + ColoredText.ANSI_RESET + user + "\n");
                    i++;// contatore per l'elenco

                }
                SharedMethods.sendToStream(output, toSend.toString());
            }
        }

    }

    // l'utente connesso fa la rewin di un post, fallisce se ha già rewinato quel
    // post e se è lui l'autore del post
    // e ovviamente se non è loggato in winsome
    private void rewinPost(int id) {
        if (clientSession != null) {
            // l'utente è loggatoa
            String thisUser = clientSession.getUser();
            try {
                // faccio al rewin del post chimando il metodo dal SocialManager
                socialManager.rewinPost(thisUser, id);
                // gestisco ogni eccezione che lancia
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "Rewin del post eseguita, controlla il tuo blog." + ColoredText.ANSI_RESET);
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post non presente nel tuo feed." + ColoredText.ANSI_RESET);
            } catch (SameUserException michela) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Non puoi fare il rewin di un tuo post." + ColoredText.ANSI_RESET);
            } catch (InvalidOperationException michela) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Hai gia' fatto il rewin di questo post." + ColoredText.ANSI_RESET);
            } catch (UserNotFoundException michela) {
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE +
                        "C'e' stato un problema interno durante il rewin, per favore prova di nuovo.\n"
                        + ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE
                        + "Winsome si scusa per il disagio."
                        + ColoredText.ANSI_RESET);
            } catch (PostNotFoundException michela) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post insesistente, controlla l'id." + ColoredText.ANSI_RESET);
            }

        } else {
            SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                    + "Effettua prima il login. Digita \"help\" per scoprire come." + ColoredText.ANSI_RESET);
        }
    }

    // metodo per seguire un altro utente
    private void followUser(String username) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Effettua prima il login." + ColoredText.ANSI_RESET);
        } else {
            username = username.toLowerCase();
            String thisUser = clientSession.getUser().toLowerCase();
            try {
                socialManager.follow(thisUser, username);
                SharedMethods.sendToStream(output, "Adesso segui l'utente " + username + ".");
                // invio la notifica
                try {
                    // notifica di aggiornamento del follower
                    RmiCallback.followeUpdate(username, "+" + thisUser);
                } catch (RemoteException michela) {
                    michela.printStackTrace();
                }
            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "Impossibile seguire un utente non registrato." + ColoredText.ANSI_RESET);
            } catch (SameUserException e) {
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Stai cercando di seguire te stesso, sei cosi' interessante? <(.^.)>"
                                + ColoredText.ANSI_RESET);
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output, "Segui gia' l'utente " + username);
            }
        }
    }

    // metodo per smettere di seguire un altro utente
    private void unfollowUser(String username) {

        if (clientSession != null) {

            username = username.toLowerCase();
            String thisUser = clientSession.getUser();

            try {

                socialManager.unfollow(thisUser, username);
                SharedMethods.sendToStream(output, "Hai smesso di seguire l'utente " + username);

                // nodifico la lista fato client
                RmiCallback.followeUpdate(username, "-" + thisUser);

            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output, "Utente inesistente.");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output,
                        "Impossibile effettuare l'operazione, non segui l'utente " + username);
            }

        } else {
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Per favore effettua prima il login." + ColoredText.ANSI_RESET);
        }

    }

    // mostra una lista dei post pubblicati e "rewinati dall'utente"
    private void blog() {
        if (clientSession == null) {
            // se l'utente non è loggato
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Prima effettua il login." + ColoredText.ANSI_RESET);
        } else {
            String thisUser = clientSession.getUser();
            // prende il blog dell'utente tramite il socialmanager
            HashSet<Post> posts = socialManager.getBlog(thisUser);
            if (posts.size() == 0) {
                // blog vuoto
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE +
                        "Non hai pubblicato e rewinato nulla, che aspetti? Inizia a guadagnare\nPer scoprire come pubblicare un post digita \"help\"."
                        + ColoredText.ANSI_RESET);
            } else {
                // blog con almeno un post
                StringBuilder toSend = new StringBuilder();
                toSend.append("\n" + ColoredText.ANSI_WHITE_BACKGROUND + ColoredText.ANSI_PURPLE + "*****BLOG*****"
                        + ColoredText.ANSI_RESET + "\n");
                for (Post p : posts) {
                    // prende il post pronto per la stampa dal metodo di formatter del social
                    // manager
                    toSend.append(socialManager.formattedPost(p.getpostId()));
                }
                SharedMethods.sendToStream(output, toSend.toString());
            }
        }
    }

    // mostra dettagliatamete il wallet all'utente
    private void getWallet() {

        if (clientSession != null) {
            Wallet thisUserWallet = socialManager.getWallet(clientSession.getUser());
            ConcurrentLinkedQueue<WalletMovement> thisWalletMovements = thisUserWallet.getTransactionList();
            StringBuilder toSend = new StringBuilder();
            toSend.append(ColoredText.ANSI_PURPLE + "WALLET DI " + clientSession.getUser().toUpperCase()
                    + ColoredText.ANSI_RESET + "\n");
            double wallet = thisUserWallet.getWallet();
            if (wallet == 1) {
                toSend.append("Possiede:\t" + socialManager.formattedWincoin(wallet));
            } else {
                toSend.append("Possiede:\t" + socialManager.formattedWincoin(wallet));
            }
            if (thisWalletMovements.size() != 0) {
                toSend.append(ColoredText.ANSI_PURPLE + "\nTRANSAZIONI:" + ColoredText.ANSI_RESET + "\n");
                for (WalletMovement transaction : thisWalletMovements) {
                    double amount = transaction.getAmount();
                    toSend.append(transaction.getDate().toString() + "\t");
                    toSend.append(socialManager.formattedWincoin(amount) + "\nReason:\t");
                    toSend.append(transaction.getReason() + "\n");
                }
            } else {
                toSend.append(ColoredText.ANSI_PURPLE + "\nTRANSAZIONI:" + ColoredText.ANSI_RESET + "\n"
                        + "Nessuna transazione in lista.");
            }
            SharedMethods.sendToStream(output, toSend.toString());
        } else {
            SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                    + "Necessario fare il login per vedere il proprio portafogli." + ColoredText.ANSI_RESET);
        }

    }

    // metodo per la conversione del wallet di un utente in bitcoins
    private void getBitcoin() {
        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat n20df = new DecimalFormat("0.00000000000000000000");
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login per convertire il tuo wallet in bitcoin.");
        } else {
            String thisUser = clientSession.getUser();
            Wallet thisUserWallet = socialManager.getWallet(thisUser);
            StringBuilder toSend = new StringBuilder();
            double cRate = thisUserWallet.getWalletbitcoin();
            double bitcoin = cRate * thisUserWallet.getWallet();

            toSend.append(ColoredText.ANSI_PURPLE + "Il tasso di conversione in bitcoin è "
                    + ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE + n20df.format(cRate)
                    + ColoredText.ANSI_RESET + "\n");
            toSend.append(ColoredText.ANSI_PURPLE + "Il portafoglio di " + thisUser + " corrisponde a "
                    + ColoredText.ANSI_PURPLE_BACKGROUND + ColoredText.ANSI_WHITE + df.format(bitcoin)
                    + ColoredText.ANSI_RESET + ColoredText.ANSI_PURPLE
                    + " Bitcoin.\nSpiacenti anche oggi sei povero. (TT.TT)\n" + ColoredText.ANSI_RESET);
            SharedMethods.sendToStream(output, toSend.toString());
        }
    }

    // operazione di logout
    private void logout(String nickname) {
        nickname = nickname.toLowerCase();
        User user = socialManager.getUser(nickname);
        // controllo se esiste l'utente
        if (user != null) {
            ClientSession thisClientSession = WinsomeServerMain.clientSessionList.get(nickname);
            if (thisClientSession != null && thisClientSession.getSocket() == clientSocket) {
                // se l'utente ha una clientSession attiva
                WinsomeServerMain.clientSessionList.remove(nickname);
                setFlag(false);// la flag server per poter effettuare un nuovo login dopo il logout
                SharedMethods.sendToStream(output, "OK");
                return;
            }
        } else {
            // l'utente non ha una clienSession Attiva
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Non hai mai effettuato il login." + ColoredText.ANSI_RESET);
        }
    }

    // e ultimo, ma non meno importante, il metodo run della classe
    // ConnectionHandler
    public void run() {
        try {
            output = new PrintWriter(clientSocket.getOutputStream(), true);// autoflush true
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // controllo che input e output siano validi altrimenti chiudo la connessione
        if (output != null && input != null) {
            String request;
            while (true) {
                try {
                    // leggo la richiesta del client
                    request = SharedMethods.readFromStream(input);
                    // splitto comando e argomenti
                    String[] splitted = request.split(" ");
                    String op = splitted[0].toLowerCase();
                    String[] args = new String[splitted.length - 1];
                    System.arraycopy(splitted, 1, args, 0, splitted.length - 1);
                    // gestisco la richiesta
                    switch (op) {
                        case "listusers":
                            listUserCommonTags();
                            break;
                        case "listfollowing":
                            getFollowingsList();
                            break;
                        case "blog":
                            blog();
                            break;
                        case "showfeed":
                            feed();
                            break;
                        case "wallet":
                            getWallet();
                            break;
                        case "walletbtc":
                            getBitcoin();
                            break;
                        case "login":
                            if (WinsomeServerMain.clientSessionList.containsKey(args[0])) {
                                String toSend = ColoredText.ANSI_PURPLE
                                        + "Questo account e' già loggato da un altro dispositivo, impossibile accedere."
                                        + ColoredText.ANSI_RESET;
                                SharedMethods.sendToStream(output, toSend);
                                break;
                            }
                            // controllo che il comando di login sia corretto
                            if (args.length != 2) {
                                // non sono stati mandati utente e password
                                SharedMethods.sendToStream(output,
                                        ColoredText.ANSI_PURPLE
                                                + "Comando di login incorretto, digita \"help\" per un aiuto."
                                                + ColoredText.ANSI_RESET);
                                break;
                            } else {
                                // esegui il login
                                login(args[0], args[1]);
                                break;
                            }
                        case "logout":
                            if (clientSession != null) {
                                // l'utente non ha loggato
                                logout(clientSession.getUser());
                            } else {
                                SharedMethods.sendToStream(output,
                                        ColoredText.ANSI_PURPLE + "Effettua prima il login." + ColoredText.ANSI_RESET);
                            }
                            break;
                        case "follow":
                            // controllo che il comando sia corretto
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE +
                                        "Comando incorretto, consulata lista dei comandi per un aiuto."
                                        + ColoredText.ANSI_RESET);
                                break;
                            } else {
                                followUser(args[0]);
                                break;
                            }
                        case "unfollow":
                            // controllo la sintessi del comando
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output,
                                        ColoredText.ANSI_PURPLE
                                                + "Comando errato, consulata lista dei comandi per saperne di piu'."
                                                + ColoredText.ANSI_RESET);
                            } else {
                                unfollowUser(args[0]);
                            }
                            break;
                        case "post":
                            // controllo la sintass del comando
                            if (!Pattern.matches("^post\\s+\".+\"\\s+\".+\"\\s*$", request)) {
                                // in questo modo controllo anche che ci sia necesariamente almeno un carattere
                                // per il titolo
                                // e uno per il contenuto
                                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                                        + "Comando errato, consulta la lista comandi." + ColoredText.ANSI_RESET);
                            } else {
                                String[] post = new String[4];
                                post = request.split("\"");
                                // post = [post ][titolo][contenuto del post]
                                post(post);
                                break;
                            }
                            break;
                        case "rewin":
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE +
                                        "Comado errato, impossibile fare la rewin del post." + ColoredText.ANSI_RESET);
                                break;
                            } else {
                                if (!SharedMethods.isNumber(args[0])) {
                                    SharedMethods.sendToStream(output,
                                            ColoredText.ANSI_PURPLE + "Formato id post incorretto, inserisci un intero."
                                                    + ColoredText.ANSI_RESET);
                                    break;
                                }
                                rewinPost(Integer.parseInt(args[0]));
                                break;
                            }

                        case "rate":
                            if (args.length != 2) {
                                SharedMethods.sendToStream(output,
                                        ColoredText.ANSI_PURPLE + "Impossibile votare." + ColoredText.ANSI_RESET);
                                break;
                            }
                            if (!SharedMethods.isNumber(args[0])) {
                                SharedMethods.sendToStream(output,
                                        ColoredText.ANSI_PURPLE
                                                + "Formato dell'id del post incorretto, inserisci un intero."
                                                + ColoredText.ANSI_RESET);
                                break;
                            }
                            ratePost(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                            break;
                        case "delete":
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                                        + "Comando errato, impossibile cancellare il post." + ColoredText.ANSI_RESET);
                            } else {
                                if (!SharedMethods.isNumber(args[0])) {
                                    SharedMethods.sendToStream(output,
                                            ColoredText.ANSI_PURPLE
                                                    + "Formato dell'id del post incorretto, deve essere un intero."
                                                    + ColoredText.ANSI_RESET);
                                    break;
                                }
                                deletePost(Integer.parseInt(args[0]));
                            }
                            break;
                        case "comment":
                            // request è composta da: comment+idpost+"testoDElCommento"
                            String[] string = new String[2];
                            string = request.split("\"");
                            // facendo la split viene fuori: [comment idpost][testoDelCommento]
                            if (request.split("\"").length < 2) {
                                SharedMethods.sendToStream(output,
                                        "Impossibile commentare, comando errato, ricordati di inesrire il testo tra "
                                                + ColoredText.ANSI_WHITE_BACKGROUND + "\" \"" + ColoredText.ANSI_RESET);
                            } else {
                                // string [0] =[comment idpost]
                                if (!SharedMethods.isNumber(string[0].split(" ")[1])) {
                                    SharedMethods.sendToStream(output,
                                            ColoredText.ANSI_PURPLE
                                                    + "Formato incorretto dell'id del post, deve essere un intero."
                                                    + ColoredText.ANSI_RESET);
                                    break;
                                }
                                int id = Integer.parseInt(string[0].split(" ")[1]);
                                comment(id, string[1]);
                            }
                            break;
                        case "showpost":
                            if (!SharedMethods.isNumber(args[0])) {
                                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                                        + "Per favore ricontrolla l'id del post, ti ricordo che deve essere una intero."
                                        + ColoredText.ANSI_RESET);
                                break;
                            }
                            getPostById(Integer.parseInt(args[0]));
                            break;
                        default:
                            // comando sconosciuto
                            unknownCmd();
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } else {
            System.err.println(ColoredText.ANSI_PURPLE + "Errore durante la creazione della connessione."
                    + ColoredText.ANSI_RESET);

        }
    }

    // mostra il post di un utente dato il suo id
    private void getPostById(int id) {
        if (clientSession != null) {

            Post post = socialManager.getPost(id);
            // controllo che il post esista
            if (post != null) {

                // il post esiste
                String toSend = "\n" + socialManager.formattedPost(id);
                SharedMethods.sendToStream(output, toSend + "\n");

            } else {
                // post non trovato
                SharedMethods.sendToStream(output,
                        ColoredText.ANSI_PURPLE + "Post inesistente." + ColoredText.ANSI_RESET);
            }
        } else {
            // clientSession = null
            SharedMethods.sendToStream(output,
                    ColoredText.ANSI_PURPLE + "Fai prima il login." + ColoredText.ANSI_RESET);
        }

    }
}
