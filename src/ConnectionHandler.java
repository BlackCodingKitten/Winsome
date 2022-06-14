import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Text;

import java.util.Objects;

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
    private final ConfigReader configReader;
    private final Socket clientSocket;
    private ClientSession clientSession;
    private final int key;
    private final SocialManager socialManager;
    private PrintWriter output;
    private BufferedReader input;

    // Costruttore della classe connection handler
    public ConnectionHandler(Socket c, int k, ConfigReader co, SocialManager s) {
        this.clientSession = null;
        this.configReader = co;
        this.socialManager = s;
        this.clientSocket = c;
        this.key = k;
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
            if (thisClientSession != null) {
                if (thisClientSession.getSocket() == clientSocket) {
                    // sessione già esistente per questo client
                    SharedMethods.sendToStream(output,
                            "Login gia' effettuato per questo account. Sara' il tuo gemello cattivo?");
                } else {
                    // non esiste una sessione per questo client
                    Date date = new Date();
                    SharedMethods.sendToStream(output, "Login effettuato correttamente.\nBentornato oggi è "
                            + date.toString() + " ora che ci sei inizia la festa.\n\\_(*w*)_/\\_(^-^)_/\\_(o.o)_/");
                }
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
            HashSet<Post> feed = (HashSet) socialManager.getUserFeed(thisUser);
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
            HashSet<String> thisUserTags = (HashSet) socialManager.getUser(thisUser).getTags();
            // inutile il controllo se
            // l'utente ha almeno un tag, ottengo tutti quelli con almemo un tag uguale
            HashSet<User> usersCommonTagList = (HashSet) socialManager.getCommonTagsUsers(thisUserTags);
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
    private void post(ArrayList<String> postArgument) {
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login prima di inviare un post.");
        } else {
            String thisUser = clientSession.getUser();
            String postTitle = postArgument.get(0).trim();
            String post = null;
            if (postArgument.size() > 1) {
                post = postArgument.get(1).trim();
            } else {
                try {
                    int idPost = socialManager.createNewPost(thisUser, postTitle, post);
                    SharedMethods.sendToStream(output, "Post " + idPost + " pubblicato correttamente");
                } catch (PostLengthException e) {
                    SharedMethods.sendToStream(output, "Inserisci un titolo più corto.");
                } catch (UserNotFoundException e) {
                    SharedMethods.sendToStream(output, "Utente insesistente.");
                }
            }
        }
    }

    // mostra il post di un utente dato il suo id
    private void getPostById(int id) {
        if (clientSession != null) {
            Post post = socialManager.getPost(id);
            // controllo che il post esista
            if (post != null) {
                // il post esiste
                String toSend = socialManager.formattedPost(post.getpostId());
                SharedMethods.sendToStream(output, toSend + "\n");
            } else {
                // post non trovato
                SharedMethods.sendToStream(output, "Post inesistente.");
            }
        } else {
            // clientSession = null
            SharedMethods.sendToStream(output, "Fai prima il login.");
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
                SharedMethods.sendToStream(output, "Hai votato " + vote + " il post " + id + ".");
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
                socialManager.deletePost(id, clientSession.getUser());
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
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua il login prima.");
        } else {
            String thisUser = clientSession.getUser();
            HashSet<String> followings = (HashSet) socialManager.getFollowings(thisUser);
            if (followings.size() == 0) {
                SharedMethods.sendToStream(output, "Non segui nessun utente.");
            } else {
                StringBuilder toSend = new StringBuilder();
                toSend.append(ColoredText.ANSI_PURPLE + "LISTA DEI SEGUITI:" + ColoredText.ANSI_RESET);
                int i = 1;
                for (String user : followings) {
                    output.append(i + ")" + user + "\n");
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
            } catch (PostNotInFeedException e) {
                SharedMethods.sendToStream(output, "Post non presente nel tuo feed.");
            } catch (SameUserException michela) {
                SharedMethods.sendToStream(output, "Non puoi fare il rewin di un tuo post.");
            } catch (InvalidOperationException michela) {
                SharedMethods.sendToStream(output, "Hai gia' fatto il rewin di quasto post.");
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
        if (clientSession == null) {
            SharedMethods.sendToStream(output, "Effettua prima il login.");
        } else {
            username = username.toLowerCase();
            String thisUser = clientSession.getUser();
            try {
                socialManager.follow(thisUser, username);
                SharedMethods.sendToStream(output, "Adesso segui l'utente " + username + ".");
                // invio la notifica
                try {
                    RmiCallback.followeUpdate(username, "+" + thisUser);
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
        if (clientSession != null) {
            username = username.toLowerCase();
            String thisUser = clientSession.getUser();
            try {
                socialManager.unfollow(thisUser, username);
                SharedMethods.sendToStream(output, "Hai smesso di seguire l'utente " + username);
                // mando la notifica
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
            SharedMethods.sendToStream(output, "Per favore effettua prima il login.");
        }
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
                toSend.append(ColoredText.ANSI_WHITE + "TRANSAZIONI:" + ColoredText.ANSI_RESET + "\n");
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
            double bitcoin = thisUserWallet.getWalletbitcoin();
            double cRate = SharedMethods.approximateDouble((bitcoin / thisUserWallet.getWallet()));
            // approssimo le cifre decimali
            bitcoin = SharedMethods.approximateDouble(bitcoin);
            toSend.append("Il tasso di conversione in bitcoin è " + cRate + "\n");
            toSend.append("Il portafoglio di " + thisUser + " corrisponde a " + ColoredText.ANSI_PURPLE + bitcoin
                    + ColoredText.ANSI_RESET + " bitcoin.\nSpiacenti anche oggi sei povero. (TT.TT)\n");
            SharedMethods.sendToStream(output, toSend.toString());
        }
    }

    // operazione di logout
    private void logout(String nickname) {
        nickname = nickname.toLowerCase();
        User user = socialManager.getUser(nickname);
        // controllo se esiste l'utente
        if (user != null) {
            if (WinsomeServerMain.clientSessionList.get(nickname) != null) {
                // se l'utente ha una clientSession attiva
                ClientSession thisClientSession = WinsomeServerMain.clientSessionList.get(nickname);
                if (thisClientSession.getSocket() == clientSocket) {
                    WinsomeServerMain.clientSessionList.remove(nickname);
                    SharedMethods.sendToStream(output, "OK");
                } else {
                    SharedMethods.sendToStream(output, "Non hai mai effettuato il login.");
                }
            } else {
                // l'utente non ha una clienSession Attiva
                SharedMethods.sendToStream(output, "Non hai mai effettuato il login.");
            }
        } else {
            SharedMethods.sendToStream(output, "Utente non trovato, per favore registrati prima ed effettua il login.");
        }
    }

    // e ultimo, ma non meno importante, il metodo run della classe
    // ConnectionHandler
    public void run(){
        try {
            output=new PrintWriter(clientSocket.getOutputStream(), true);//autoflush true
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) { 
            e.printStackTrace();
        }

        //controllo che input e output siano validi altrimenti chiudo la connessione
        if(output != null && input != null){
            String request;
            while(!false){
                try{
                    //leggo la richiesta del client
                    request= SharedMethods.readFromStream(input);
                    //splitto comando e argomenti
                    String[] splitted = request.split(" ");
                    String op = splitted[0];
                    String[] args = new String[splitted.length -1 ];
                    System.arraycopy(splitted, 1, args, 0, splitted.length -1 );
                    //gestisco la richiesta
                    switch(op){
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
                        case "walletbitcoin":
                            //DEBUG.messaggioDiDebug("wallet bitcoin");
                            getBitcoin();
                            break;
                        case "login":
                            //controllo che il comando di login sia corretto
                            if(args.length != 2){
                                //non sono stati mandati utente e password
                                SharedMethods.sendToStream(output, "Comando di login incorretto, digita \"help\" per un aiuto.");
                                break;
                            }else{
                                login(args[0], args[1]);
                                break;
                            }
                        case "logout":
                            if(clientSession != null){
                                //l'utente non ha loggato
                                logout(clientSession.getUser());
                            }else{
                                SharedMethods.sendToStream(output, "Effettua prima il login.");
                            }
                        case "follow":
                            //controllo che il comando sia corretto
                            if(args.length != 1){
                                SharedMethods.sendToStream(output, "Comando incorretto, consulata lista dei comandi per un aiuto.");
                                break;
                            }else{
                                followUser(args[0]);
                                break;
                            }
                        case "unfollow":
                            //controllo la sintessi del comando
                            if(args.length != 1){
                                SharedMethods.sendToStream(output, "Comando errato, consulata lista dei comandi per saperne di piu'.");
                            }else{
                                unfollowUser(args[1]);
                                //DEBUG.messaggioDiDebug("unfollow");
                            }
                            break;
                        case "post":
                            //controllo la sintass del comando 
                            if(!Pattern.matches("^post\\s+\".+\"\\s+\".+\"\\s*$", request)){
                                //in questo modo controllo anche che ci sia necesariamente almeno un carattere per il titolo 
                                //e uno per il contenuto
                                SharedMethods.sendToStream(output, "Comando errato, consulata la lisat comandi.");
                            }else{
                                Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(request);
                                ArrayList<String> post = new ArrayList<>();
                                while(m.find()){
                                    //ottengo i valori di post
                                    post.add(m.group(1));
                                }
                                post(post);
                                break;


                            }

                            
                        
                    }
                }catch(){

                }
            }
        }else{
            System.err.println("Errore durante la creazione della connessione.");
        }
    }
}
