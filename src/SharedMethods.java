import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.Scanner;

//Classe miscellanea di metodi per il server e il client

public class SharedMethods {
    private final static DEBUG debug = new DEBUG();

    // writeOnStream() metodo che permette di inviare una stringa su uno stream
    public static void writeOnStream(PrintWriter out, String toSend) {
        int bytes = toSend.getBytes().length;
        out.println(bytes);// invio la lunghezza della stringa
        out.print(toSend); // invio la stringa
        out.flush();
        // out è lo stream su cio inviare la stringa
    }

    // readFromStream() metodo che permette di leggere una stringa da uno stream
    public static String readFromStream(BufferedReader in) throws IOException {
        int size = 0;
        StringBuilder sBuilder = new StringBuilder();
        // leggo la lunghezza della stringa
        String length = in.readLine();
        // debug.messaggioDiDebug("Lunghezza della stringa: " + length);
        if (length == null) {
            System.out.println("*****Errore!!!*****\nStringa di lunghezza nulla.");
            return null;
        } else {

            try {
                size = Integer.parseInt(length);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                System.out.println("*****Errore!!!*****\nStringa con un formato incorretto, conversione non riuscita");
            }

        }
        for (int i = 0; i < size; i++) {
            // leggo carattere per carattere in un for grazie alla size ricevuta e lo salvo
            // nello stringBuilder
            sBuilder.append((char) in.read());
        }
        return sBuilder.toString();

    }

    // readFromConsole() così come dice il nome permette din leggere una Stringa da
    // console, sostitutivo della readline() per gestire il caso della
    // NoSuchElementException che si può verificare a causa di una terminazione
    // anticipata mentre si sta leggendo un input, il metodo restituisce la stringa
    // letta
    public static String readFromConsole(Scanner input) {
        String inputString = null;
        try {
            inputString = input.nextLine();
        } catch (NoSuchElementException e) {
            // debug.messaggioDiDebug("NoSuchElementException lanciata ");
            e.printStackTrace();
        }
        return inputString;
    }

    // metodo che controlla che la password inserita sia corretta
    public boolean isPasswordCorrect(String plaintext, String hashed) {
        if (hashed.equals(User.encryptedPassword(plaintext))) {
            return true;
        } else {
            return false;
        }
    }

}