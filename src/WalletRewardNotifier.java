import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

import javax.naming.ConfigurationException;

/*è la clsse che gestisce le notifiche di aggiornamnento dei wallet degli utenti, la notifica è uguale per tutti */

public class WalletRewardNotifier implements Runnable {
    // private static final DEBUG debug = new DEBUG();
    private boolean listen = true;

    private MulticastSocket multicastSocket;
    private /*final */ int port;
    private /*final */ InetAddress address;

    // costruttore della classe
    public WalletRewardNotifier(String address, int port) throws ConfigurationException {
        // controllo sul valore della porta altrimenti lancia Configuration exception
        if (port < 0) {
            throw new ConfigurationException("Valore della porta: " + port + " non valido!!");
        } else {
            this.port = port;
            // debug.messaggioDiDebug("Valore della porta multicast "+ this.port);
        }
        try {
            this.address = InetAddress.getByName(address);
            // debug.messaggioDiDebug("indirizzo multicast "+ address);
            // controllo che l'indirizzo multicast passato sia correttoa ltrimenti lancia
            // una Configuaration exceptions
            if (this.address.isMulticastAddress()) {
            } else {
                throw new ConfigurationException("L''indirizzo fornito: " + address + " non è valido!!");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            // debug.messaggioDiDebug("indirizzo multicast non valido");
        }
    }

    public void run() {
        // inizializzo la multicast socket
        try {
            multicastSocket = new MulticastSocket(this.port);
            InetSocketAddress group = new InetSocketAddress(this.address, this.port);
            // getByInetAddress() trova l'interfaccia che ha this.address collegato
            NetworkInterface netIf = NetworkInterface.getByInetAddress(this.address);
            // entro nel gruppo multicast
            multicastSocket.joinGroup(group, netIf);

            // mi metto in ascolto dei pacchetti in arrivo
            while (listen) {
                try {
                    // allocateDirect(capacity) rende il buffer di byte diretto, ossia la JVM non
                    // utilizzerà un buffer intermedio dove copiare il contenuto del buffer prima o
                    // dopo ogni
                    // chiamata di operazione I/O nativa del sistema, i buffer diretti hanno in
                    // genere costi di allocazione
                    // e dellocazione maggiori, ma il loro contenuto può risiedere al di fuori
                    // dell'heap del garbadge-collector,
                    // quindi l'implicazione sulla memoria dell'applicazione potrebbe non essere
                    // grave
                    // di solito i buffer diretti sono consigliati per le grandi dimensioni,
                    // ma oggi (8/06/2022) ho voglia di sperimentare
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Integer.BYTES);// contiene direttemante la
                                                                                     // lunghezza della stringa ricevuta
                    DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.limit());
                    multicastSocket.receive(datagramPacket);// il primo pacchetto contine solamente la dimensione della
                                                            // stringa successiva
                    int stringLength = ByteBuffer.wrap(datagramPacket.getData()).getInt();
                    // debug.messaggioDiDebug("Lunghezza della stringa spedita col primo
                    // pacchetto");
                    // allocazione dinamica
                    byte[] byteBufferArray = new byte[stringLength];
                    datagramPacket = new DatagramPacket(byteBufferArray, stringLength);
                    multicastSocket.receive(datagramPacket);// secondo pacchetto che contine info
                    String notif = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    /// debug.messaggioDiDebug("contenuto del secondo pacchetto: "+ notifica);
                    String message = "Sono stati emessi un totale di " + notif
                            + " in premio, controlla il tuo Wallet per vedere se ne hai ricevuti.\n***** (°u°) *****";
                    System.out.print(message + "\n>");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } // fine while I
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // metodo per terminare il loop di lettura pacchetti altrimenti il thread
    // rimarrebbe in attesa
    public void stopReading() {
        this.listen = false;
        multicastSocket.close();
    }
}
