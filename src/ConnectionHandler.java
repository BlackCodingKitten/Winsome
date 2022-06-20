import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;

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

    // Costruttore della classe connection handler
    public ConnectionHandler(Socket c, int k, ConfigReader co, SocialManager s) {
        this.clientSession = null;
        // this.configReader = co;
        this.socialManager = s;
        this.clientSocket = c;
        // this.key = k;
        this.output = null;
        this.input = null;

    }

    // comando sconosciuto
    private void unknownCmd() {
        SharedMethods.sendToStream(output, "Comando Sconosciuto.");
    }

    // metodo di login: se utente e password sono corretti e non esiste già una
    // sessione associata a quei dati, genero una nuova sessione
    private void login(String nickname, String password) {
        if (clientSession != null) {
            SharedMethods.sendToStream(output,
                    "Login gia' effettuato per questo account. Sara' il tuo gemello cattivo?");
            return;
        }
        nickname = nickname.toLowerCase();
        User user = socialManager.getUser(nickname);
        if (user != null && SharedMethods.isPasswordCorrect(password, user.getPassword())) {// controllo che nome utente
                                                                                            // e password siano corretti
            ClientSession thisClientSession = WinsomeServerMain.clientSessionList.get(nickname);
            if (thisClientSession != null && thisClientSession.getSocket() == clientSocket) {
                // sessione già esistente per questo client
                SharedMethods.sendToStream(output,
                        "Login gia' effettuato" + ColoredText.ANSI_PURPLE + thisClientSession.getUser()
                                + ColoredText.ANSI_RESET + "Sara' il tuo gemello cattivo?");
                return;
            }
            // se thisClientSession = null
            thisClientSession = new ClientSession(nickname, clientSocket);
            WinsomeServerMain.clientSessionList.put(nickname, thisClientSession);
            clientSession = thisClientSession;
            SharedMethods.sendToStream(output, "OK");
        } else {
            // nome utente password non corretti
            SharedMethods.sendToStream(output, "Errore!!\nPer favore ricontrolla nome utente e password.");
        }

    }

    // metodo che mostra il feed di un utente
    private void feed() {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua prima il login, grazie.");
        } else {
            String thisUser = clientSession.getUser();
            HashSet<Post> feed = (HashSet<Post>) socialManager.getUserFeed(thisUser);
            if (feed.size() == 0) {
                // se il feed è vuoto
                SharedMethods.sendToStream(output, "Il tuo feed è vuoto.\nSei un nuovo utente?, digita \"help\"");
                return;
            }
            // se il feed ha almeno 1 post
            StringBuilder toSend = new StringBuilder();
            toSend.append(ColoredText.ANSI_PURPLE + "******FEED DI " + ColoredText.ANSI_WHITE_BACKGROUND
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
            SharedMethods.sendToStream(output, "Login non effettuato.");
        } else {
            String thisUser = clientSession.getUser();
            HashSet<String> thisUserTags = (HashSet<String>) socialManager.getUser(thisUser).getTags();
            // inutile il controllo se
            // l'utente ha almeno un tag, ottengo tutti quelli con almemo un tag uguale
            HashSet<User> usersCommonTagList = (HashSet<User>) socialManager.getCommonTagsUsers(thisUserTags);
            // rimuovo l'utente stesso dalla lista
            usersCommonTagList.remove(socialManager.getUser(thisUser));
            if (usersCommonTagList.size() == 0) {
                SharedMethods.sendToStream(output, "Nessun utente ha tag in comune con te. (TT.TT)");
                return;
            }
            StringBuilder toSend = new StringBuilder();
            toSend.append(ColoredText.ANSI_WHITE + ColoredText.ANSI_PURPLE
                    + "LISTA DI UTENTI CHE HANNO TAG IN COMUNE CON TE:" + ColoredText.ANSI_RESET + "\n");
            for (User user : usersCommonTagList) {
                toSend.append(ColoredText.ANSI_PURPLE + user.getNickname() + " " + ColoredText.ANSI_RESET + "\t[");
                for (String tag : user.getTags()) {
                    // append()dei tag in comune
                    if (thisUserTags.contains(tag)) {
                        toSend.append(tag + "-");
                    }
                }
                toSend.append("\n");
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
            SharedMethods.sendToStream(output, "Effettua il login prima di inviare un post.");
        } else {
            String thisUser = clientSession.getUser();
            DEBUG.messaggioDiDebug(thisUser);
            String postTitle = postArgument[1];
            DEBUG.messaggioDiDebug("titolo: " + postTitle);
            String post = postArgument[3];
            DEBUG.messaggioDiDebug(post);

            try {
                int idPost = socialManager.createNewPost(thisUser, postTitle, post);
                SharedMethods.sendToStream(output, "Post " + idPost + " pubblicato correttamente");
                try {
                    WinsomeServerMain.fileManager.fileSaver("config/jsonFile" + "/" + "post.json",
                            socialManager.getPostList());
                } catch (IOException e) {
                    DEBUG.messaggioDiDebug("impossibile salvare su file");
                }
            } catch (PostLengthException e) {
                SharedMethods.sendToStream(output, "Inserisci un titolo più corto.");
            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output, "Utente insesistente.");
            }
        }
    }

    // metodo per votare un post
    private void ratePost(int id, int vote) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Prima di poter votare devi effetttuare il login.");
        } else {
            try {
                String thisUser = clientSession.getUser();
                socialManager.ratePost(thisUser, id, vote);
                if (vote > 0) {
                    SharedMethods.sendToStream(output, "Hai votato +" + vote + " il post " + id + ".");
                } else {
                    SharedMethods.sendToStream(output, "Hai votato " + vote + " il post " + id + "."
                            + "\nCommenta per dire cosa non ti è piaciuto.");
                }

            } catch (InvalidVoteValueException e) {
                SharedMethods.sendToStream(output, "Voto non valido.");
            } catch (UserNotFoundException e) {
                // errore del server
                SharedMethods.sendToStream(output, "Errore interno, per favore riprova.");
                e.printStackTrace();
            } catch (PostNotFoundException e) {
                SharedMethods.sendToStream(output, "Il post che vuoi votare non esiste.");
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output, "Questo post non è nel tuo feed.");
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output, "Hai già votato questo post.");
            } catch (SameUserException e) {
                SharedMethods.sendToStream(output, "Non puoi votare il tuo stesso post.");
            }
        }
    }

    // metodo per commentare un post
    private void comment(int id, String comment) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua prima il login.");
        } else {
            try {
                socialManager.commentPost(clientSession.getUser(), id, comment);
                SharedMethods.sendToStream(output, "Commento pubblicato correttamente.");
            } catch (PostNotFoundException | PostNotInFeedException e) {
                SharedMethods.sendToStream(output, "Impossibile pubblicare il commento.");
            }
        }
    }

    // metodo per cancellare un post
    private void deletePost(int id) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login prima di cancellare un post.");
        } else {
            try {
                String thisUser = clientSession.getUser();
                socialManager.deletePost(id, thisUser);
                SharedMethods.sendToStream(output, "Post eliminato correttamente.");
            } catch (PostNotFoundException e) {
                SharedMethods.sendToStream(output, "Post da eliminare non trovato.");
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output, "Puoi elimnare solo i post che hai scritto te.");
            }
        }
    }

    // stampa invia la lista dei segiti dell'utente o un messaggio che dice lista
    // vuota se non ne segue nessuno
    private void getFollowingsList() {
        DEBUG.messaggioDiDebug("");
        StringBuilder toSend = new StringBuilder();
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login prima.");
            return;
        } else {
            String thisUser = clientSession.getUser();
            DEBUG.messaggioDiDebug("copio la lista dei following ");
            HashSet<String> followings = (HashSet<String>) socialManager.getFollowings(thisUser);
            if (followings.size() == 0) {
                SharedMethods.sendToStream(output, "Non segui nessun utente.");
                return;
            } else {
                DEBUG.messaggioDiDebug("la lista dei following non è vuota");
                toSend.append(ColoredText.ANSI_PURPLE + "LISTA DEI SEGUITI:\n" + ColoredText.ANSI_RESET);
                int i = 1;
                for (String user : followings) {
                    DEBUG.messaggioDiDebug(user);
                    toSend.append(i + ")" + user + "\n");
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
        DEBUG.messaggioDiDebug("rewin nel connection handler");
        if (clientSession != null) {
            DEBUG.messaggioDiDebug("la client session esiste");
            // l'utente è loggatoa
            String thisUser = clientSession.getUser();
            try {
                DEBUG.messaggioDiDebug("sto per eseguire il metodo del social manager");
                // faccio al rewin del post chimando il metodo dal SocialManager
                socialManager.rewinPost(thisUser, id);
                // gestisco ogni eccezione che lancia
                SharedMethods.sendToStream(output, ColoredText.ANSI_PURPLE
                        + "REwin del post eseguita, controlla il tuo blog." + ColoredText.ANSI_RESET);
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output, "Post non presente nel tuo feed.");
            } catch (SameUserException michela) {
                SharedMethods.sendToStream(output, "Non puoi fare il rewin di un tuo post.");
            } catch (InvalidOperationException michela) {
                SharedMethods.sendToStream(output, "Hai gia' fatto il rewin di questo post.");
            } catch (UserNotFoundException michela) {
                SharedMethods.sendToStream(output,
                        "C'e' stato un problema interno durante il rewin, per favore prova di nuovo.\n"
                                + ColoredText.ANSI_PURPLE_BACKGROUND + "Winsome si scusa per il disagio."
                                + ColoredText.ANSI_RESET);
            } catch (PostNotFoundException michela) {
                SharedMethods.sendToStream(output, "Post insesistente, controlla l'id.");
            }

        } else {
            SharedMethods.sendToStream(output, "Effettua prima il login. Digita \"help\" per scoprire come.");
        }
    }

    // metodo per seguire un altro utente
    private void followUser(String username) {
        DEBUG.messaggioDiDebug("sono in follow user");
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua prima il login.");
        } else {
            DEBUG.messaggioDiDebug("client session esiste");
            username = username.toLowerCase();
            String thisUser = clientSession.getUser().toLowerCase();
            try {
                DEBUG.messaggioDiDebug("prima di eseguire le operazioni nel socialmanager");
                socialManager.follow(thisUser, username);
                DEBUG.messaggioDiDebug("ha eseguito il metodo nel social manager");
                SharedMethods.sendToStream(output, "Adesso segui l'utente " + username + ".");
                // invio la notifica
                try {
                    DEBUG.messaggioDiDebug("RMI");
                    RmiCallback.followeUpdate(username, "+" + thisUser);
                    DEBUG.messaggioDiDebug("Rmi eseguito");
                } catch (RemoteException michela) {
                    michela.printStackTrace();
                }
            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output, "Impossibile seguire un utente non registrato.");
            } catch (SameUserException e) {
                SharedMethods.sendToStream(output,
                        "Stai cercando di seguire te stesso, sei cosi' interessante? <(.^.)>");
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output, "Segui gia' l'utente " + username);
            }
        }
    }

    // metodo per smettere di seguire un altro utente
    private void unfollowUser(String username) {
        DEBUG.messaggioDiDebug("sono dentro unfollow user nel connection handler");
        if (clientSession != null) {
            DEBUG.messaggioDiDebug("la client session non è null");
            username = username.toLowerCase();
            String thisUser = clientSession.getUser();
            DEBUG.messaggioDiDebug(thisUser);
            try {
                DEBUG.messaggioDiDebug("Sto per entrare nel social manager");
                socialManager.unfollow(thisUser, username);
                SharedMethods.sendToStream(output, "Hai smesso di seguire l'utente " + username);
                DEBUG.messaggioDiDebug("Sono uscita dal social manager");
                // nodifico la lista fato client
                RmiCallback.followeUpdate(username, "-" + thisUser);
                DEBUG.messaggioDiDebug("Processo Rmi finito");
            } catch (UserNotFoundException e) {
                SharedMethods.sendToStream(output, "Utente inesistente.");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (InvalidOperationException e) {
                SharedMethods.sendToStream(output,
                        "Impossibile effettuare l'operazione, non segui l'utente " + username);
            }

        } else {
            SharedMethods.sendToStream(output, "Per favore effettua prima il login.");
        }
        DEBUG.messaggioDiDebug("Esco dalla funzione");
    }

    // mostra una lista dei post pubblicati e "rewinati dall'utente"
    private void blog() {
        if (clientSession == null) {
            // se l'utente non è loggato
            SharedMethods.sendToStream(output, "Prima effettua il login.");
        } else {
            String thisUser = clientSession.getUser();
            // prende il blog dell'utente tramite il socialmanager
            HashSet<Post> posts = socialManager.getBlog(thisUser);
            if (posts.size() == 0) {
                // blog vuoto
                SharedMethods.sendToStream(output,
                        "Non hai pubblicato e rewinato nulla, che aspetti? Inizia a guadagnare\nPer scoprire come pubblicare un post digita \"help\".");
            } else {
                // blog con almeno un post
                StringBuilder toSend = new StringBuilder();
                toSend.append(ColoredText.ANSI_BLACK_BACKGROUND + ColoredText.ANSI_PURPLE + "*****BLOG*****"
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
            double wallet = SharedMethods.approximateDouble(thisUserWallet.getWallet());
            if (wallet == 1) {
                toSend.append("Possiede:\t" + wallet + " Wincoin");
            } else {
                toSend.append("Possiede:\t" + wallet + " Wincoins");
            }
            if (thisWalletMovements.size() != 0) {
                toSend.append(ColoredText.ANSI_PURPLE + "\nTRANSAZIONI:" + ColoredText.ANSI_RESET + "\n");
                for (WalletMovement transaction : thisWalletMovements) {
                    double amount = SharedMethods.approximateDouble(transaction.getAmount());
                    toSend.append(transaction.getDate().toString() + "\t");
                    toSend.append(amount + "\nReason:\t");
                    toSend.append(transaction.getReason() + "\n");
                }
            } else {
                toSend.append("Nessuna transazione in lista.");
            }
            SharedMethods.sendToStream(output, toSend.toString());
        } else {
            SharedMethods.sendToStream(output, "Necessario fare il login per vedere il proprio portafogli.");
        }

    }

    // metodo per la conversione del wallet di un utente in bitcoins
    private void getBitcoin() {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login per convertire il tuo wallet in bitcoin.");
        } else {
            String thisUser = clientSession.getUser();
            Wallet thisUserWallet = socialManager.getWallet(thisUser);
            StringBuilder toSend = new StringBuilder();
            double cRate = thisUserWallet.getWalletbitcoin();
            // approssimo a 4 cifre decimali
            cRate = SharedMethods.approximateDouble(cRate);
            // approssimo le cifre decimali
            double bitcoin = cRate * thisUserWallet.getWallet();
            // approissimo a 4 cifre decimali
            bitcoin = SharedMethods.approximateDouble(bitcoin);
            toSend.append(ColoredText.ANSI_PURPLE + "Il tasso di conversione in bitcoin è "
                    + ColoredText.ANSI_WHITE_BACKGROUND + cRate + ColoredText.ANSI_RESET + "\n");
            toSend.append(ColoredText.ANSI_PURPLE + "Il portafoglio di " + thisUser + " corrisponde a "
                    + ColoredText.ANSI_WHITE_BACKGROUND + bitcoin
                    + ColoredText.ANSI_RESET + ColoredText.ANSI_PURPLE
                    + " bitcoin.\nSpiacenti anche oggi sei povero. (TT.TT)\n" + ColoredText.ANSI_RESET);
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
                SharedMethods.sendToStream(output, "OK");
            }
            return;
        } else {
            // l'utente non ha una clienSession Attiva
            SharedMethods.sendToStream(output, "Non hai mai effettuato il login.");
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
                    String op = splitted[0];
                    String[] args = new String[splitted.length - 1];
                    System.arraycopy(splitted, 1, args, 0, splitted.length - 1);
                    // gestisco la richiesta
                    switch (op) {
                        case "listusers":
                            DEBUG.messaggioDiDebug("listusers");
                            listUserCommonTags();
                            break;
                        case "listfollowing":
                            DEBUG.messaggioDiDebug("list following nel connection handler");
                            getFollowingsList();
                            DEBUG.messaggioDiDebug("fuori da list following nel connection handler");
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
                            DEBUG.messaggioDiDebug("wallet bitcoin");
                            getBitcoin();
                            break;
                        case "login":
                            // controllo che il comando di login sia corretto
                            if (args.length != 2) {
                                // non sono stati mandati utente e password
                                SharedMethods.sendToStream(output,
                                        "Comando di login incorretto, digita \"help\" per un aiuto.");
                                break;
                            } else {
                                login(args[0], args[1]);
                                break;
                            }
                        case "logout":
                            if (clientSession != null) {
                                // l'utente non ha loggato
                                logout(clientSession.getUser());
                            } else {
                                SharedMethods.sendToStream(output, "Effettua prima il login.");
                            }
                            break;
                        case "follow":
                            // controllo che il comando sia corretto
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output,
                                        "Comando incorretto, consulata lista dei comandi per un aiuto.");
                                break;
                            } else {
                                followUser(args[0]);
                                break;
                            }
                        case "unfollow":
                            DEBUG.messaggioDiDebug("Unfollow nello switch del connection handler");
                            // controllo la sintessi del comando
                            if (args.length != 1) {
                                DEBUG.messaggioDiDebug("argomenti <1");
                                SharedMethods.sendToStream(output,
                                        "Comando errato, consulata lista dei comandi per saperne di piu'.");
                            } else {
                                DEBUG.messaggioDiDebug("Sto per svolgere il metodo nel connection handler");
                                unfollowUser(args[0]);
                                DEBUG.messaggioDiDebug("unfollow completato");
                            }
                            break;
                        case "post":
                            // controllo la sintass del comando
                            if (!Pattern.matches("^post\\s+\".+\"\\s+\".+\"\\s*$", request)) {
                                // in questo modo controllo anche che ci sia necesariamente almeno un carattere
                                // per il titolo
                                // e uno per il contenuto
                                SharedMethods.sendToStream(output, "Comando errato, consulta la lista comandi.");
                            } else {
                                String[] post = new String[4];
                                post = request.split("\"");
                                DEBUG.messaggioDiDebug(
                                        post[0] + "\n" + "Titolo: " + post[1] + "\n" + post[2] + "\n" + post[3]);
                                post(post);
                                break;
                            }
                            break;
                        case "rewin":
                            DEBUG.messaggioDiDebug("rewin nel connection handler");
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output,
                                        "Comado errato, impossibile fare la rewin del post.");
                                break;
                            } else {
                                DEBUG.messaggioDiDebug("Sto per entrare nel socialmanager e fare la rewin del post "
                                        + ColoredText.ANSI_BLACK_BACKGROUND + args[0] + ColoredText.ANSI_RESET);
                                rewinPost(Integer.parseInt(args[0]));
                                DEBUG.messaggioDiDebug("sono uscito dalla rewin nel connection handler");
                                break;
                            }

                        case "rate":
                            if (args.length != 2) {
                                SharedMethods.sendToStream(output, "Impossibile votare.");
                                break;
                            }
                            ratePost(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                            break;
                        case "delete":
                            if (args.length != 1) {
                                SharedMethods.sendToStream(output, "Comando errato, impossibile cancellare il post.");
                            } else {
                                deletePost(Integer.parseInt(args[0]));
                            }
                            break;
                        case "comment":
                            DEBUG.messaggioDiDebug("CommentSwitch");
                            // request è composta da: comment+idpost+"testoDElCommento"
                            String[] string = new String[2];
                            string = request.split("\"");
                            DEBUG.messaggioDiDebug("string [0] " + string[0]);
                            DEBUG.messaggioDiDebug("String [1] " + string[1]);
                            // facendo la split viene fuori: [comment idpost][testoDelCommento]
                            if (request.split("\"").length < 2) {
                                SharedMethods.sendToStream(output, "Impossibile commentare, comando errato.");
                            } else {
                                // string [0] =[comment idpost]
                                int id = Integer.parseInt(string[0].split(" ")[1]);
                                DEBUG.messaggioDiDebug("id post: " + String.valueOf(id));
                                comment(id, string[1]);
                            }
                            break;
                        case "showpost":
                            DEBUG.messaggioDiDebug("showpost");
                            DEBUG.messaggioDiDebug("showpost 2");
                            /*
                             * if (args.length < 1) {
                             * DEBUG.messaggioDiDebug("args < 1");
                             * SharedMethods.sendToStream(output,
                             * "Comando errato, impossibile mostrare il post.");
                             * } else {
                             */
                            DEBUG.messaggioDiDebug("id del post cercato: " + args[0]);
                            getPostById(Integer.parseInt(args[0]));
                            // }
                            break;
                        default:
                            unknownCmd();
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } else {
            System.err.println("Errore durante la creazione della connessione.");

        }
    }

    // mostra il post di un utente dato il suo id
    private void getPostById(int id) {
        if (clientSession != null) {
            DEBUG.messaggioDiDebug("la client session esite.");
            Post post = socialManager.getPost(id);
            // controllo che il post esista
            if (post != null) {
                DEBUG.messaggioDiDebug("il post esiste");
                // il post esiste
                String toSend = socialManager.formattedPost(id);
                SharedMethods.sendToStream(output, toSend + "\n");

            } else {
                // post non trovato
                SharedMethods.sendToStream(output, "Post inesistente.");
            }
        } else {
            // clientSession = null
            SharedMethods.sendToStream(output, "Fai prima il login.");
        }
        DEBUG.messaggioDiDebug("getpostid fine");
    }
}
