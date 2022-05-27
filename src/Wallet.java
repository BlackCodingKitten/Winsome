import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Wallet {
    public User owner;
    public double walletAmount = 0;

    public double getWallet() {
        return walletAmount;
    }

    public double getWalletbitcoin() {

        String valueConverter = null;
        URL rdoUrl = new URL(
                "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=html&rnd=new");
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) rdoUrl.openConnection();
        InputStream stream = rdoUrl.openStream();
        int status = httpsUrlConnection.getResponseCode();
        if (status < 300) {
            stream = new BufferedInputStream(stream);
            Reader reader = new InputStreamReader(stream);
            int c;

            while ((c = reader.read()) != -1) {
                System.out.print((char) c);
            }
        } else {
            Reader streamReader = null;
            streamReader = new InputStreamReader(httpsUrlConnection.getErrorStream());
        }

    }

    }

    public double amountUpdate() {

    }

}