package CustomizedException;

public class InvalidVoteValueException extends Throwable {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public InvalidVoteValueException() {
        super(ANSI_RED + "\nValore del voto errato. Digita il comando \"help\" per sapere come votare un post.\n"
                + ANSI_RESET);
    }
}