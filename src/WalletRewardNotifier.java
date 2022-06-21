import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
/*è la clsse che gestisce le notifiche di aggiornamnento dei wallet degli utenti, la notifica è uguale per tutti */

import Color.ColoredText;

public class WalletRewardNotifier implements Runnable {
    // private static final DEBUG debug = new DEBUG();
    private boolean listen = true;
    private MulticastSocket multicastSocket;
    private /* final */ int port;
    private /* final */ InetAddress address;

    // costruttore della classe
    public WalletRewardNotifier(String address, int port) {
        this.port = port;
        try {
            this.address = InetAddress.getByName(address);
            // controllo che l'indirizzo multicast passato sia correttoa ltrimenti lancia
            // una Configuaration exceptions

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    // metodo run che implementa la ricezione dei reward con UDP multicast
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
            while (true) {
                try {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                    // contiene direttemante la lunghezza della stringa ricevuta
                    DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.limit());
                    multicastSocket.receive(datagramPacket);
                    // il primo pacchetto contine solamente la dimensione della stringa successiva
                    int length = ByteBuffer.wrap(datagramPacket.getData()).getInt();
                    // allocazione
                    ByteBuffer buffer = ByteBuffer.allocate(length);
                    datagramPacket = new DatagramPacket(buffer.array(), buffer.limit());
                    multicastSocket.receive(datagramPacket);// secondo pacchetto che contine info
                    String notif = new String(datagramPacket.getData(), datagramPacket.getOffset(),
                            datagramPacket.getLength());
                    String message = "Sono stati emessi un totale di " + notif
                            + "Wincoins in premio, usa il comando \"wallet\" per vedere se ne hai ricevuti.\n***** (°u°) *****";
                    System.out.print(
                            ColoredText.ANSI_PURPLE + "\nNUOVA NOTIFICA: " + message + "\n" + ColoredText.ANSI_RESET);
                    if (!listen) {
                        multicastSocket.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // metodo per terminare il loop di lettura pacchetti altrimenti il thread
    // rimarrebbe in attesa, sia ttiva quando chiude il client
    public void stopReading() {
        this.listen = false;
    }
}
