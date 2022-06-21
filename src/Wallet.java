import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.HttpsURLConnection;

/*rappresenta il wallet di un utente, il valore corrente viene calcolato dalla lista di transazioni che l'untente ha fatto */
public class Wallet {
    //private static final DEBUG debug = new DEBUG();
    private final String owner; //-> proprietario del portafoglio
    private final ConcurrentLinkedQueue<WalletMovement> transaction; //-> lista delle transazioni del portafoglio
    private double walletAmount;//-> valore corrente del portafoglio riaggiornato ad ogni chiamata

    // costruttore della classe wallet
    public Wallet(String owner) {
        this.owner = owner;
        this.transaction = new ConcurrentLinkedQueue<WalletMovement>();
        this.walletAmount = 0;

    }

    // metodo getter del proprietario del portafoglio
    public String getOwner() {
        return this.owner;
    }

    // metodo getter della lista delle transazioni fatte dall'utente dalla più
    // vecchia alla più nuova
    public ConcurrentLinkedQueue<WalletMovement> getTransactionList() {
        return this.transaction;
    }

    // metodo getter del valore del portafoglio
    public double getWallet() {
        this.walletAmount = getTotalAmount();
        return SharedMethods.approximateDouble(this.walletAmount);
    }

    // calcola il totale del valore del portafoglio
    private double getTotalAmount() {
        double amount = 0.000;
        for (WalletMovement m : transaction) {
            amount = amount + m.getAmount();
        }
        return SharedMethods.approximateDouble(amount);
    }

    // metodo che permette di convertire la moneta winsome in bitcoin con un tasso
    // di cambio di cambio preso via url da Random.org
    public double getWalletbitcoin() {
        double exchangeRate = 0.000;
        try {
            URL url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=4&col=1&format=plain&rnd=new");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(); //httpsURLConnection estende httpURLConnection con funzionalità specifiche per https
            if (connection.getResponseCode() < 300) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bReader.readLine();
                exchangeRate = Double.parseDouble(line);
            } else {
                System.out.print(" Servizio temporaneamente non disponibile. :-( \nRiprovare più tardi \n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exchangeRate;
    }

    // calcola e aggiorna il valore del portafoglio quando si fa un movimento,
    // positivo o negativo che sia
    public double amountUpdate(String reason, double movementAmount) {
        this.transaction.add(new WalletMovement(reason, movementAmount));
        this.walletAmount = this.walletAmount + movementAmount;
        return this.walletAmount;
    }


    @Override
    public int hashCode(){
        return this.owner.hashCode();
    }
}