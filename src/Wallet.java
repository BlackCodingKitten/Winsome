import java.io.BufferedReader;
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
        URL rdoUrl = new URL(
                "https://www.random.org/integers/?num=1&min=1&max=1000000000&col=1&base=10&format=html&rnd=new");
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) rdoUrl.openConnection();
        httpsUrlConnection.setRequestMethod("GET");
        httpsUrlConnection.setConnectTimeout(5000);
        httpsUrlConnection.setReadTimeout(5000);
        int status = httpsUrlConnection.getResponseCode();
        if (status <= 299) {
            BufferedReader in = new BufferedReader(new InputStreamReader(httpsUrlConnection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
        } else {
            Reader streamReader = null;
            streamReader = new InputStreamReader(httpsUrlConnection.getErrorStream());
        }
        return;
    }

    public double amountUpdate() {

    }

}