import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Wallet {
    public User owner;
    public double walletAmount = 0;

    public double getWallet() {
        return walletAmount;
    }

    public double getWalletbitcoin() {
        double exchangeRate = 0.0000;
        try {
            URL url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=4&col=1&format=plain&rnd=new");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            if (connection.getResponseCode() < 300) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bReader.readLine();
                exchangeRate = Double.parseDouble(line);
            } else {
                System.out.print(" Service temporarily unavailable. :-( \nPlease try again later \n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.walletAmount * exchangeRate;
    }

    public double amountUpdate() {
        return this.walletAmount;
    }

}