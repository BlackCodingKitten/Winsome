package CustomizedException;

public class UserNotFoundException extends Throwable {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public UserNotFoundException() {
        super(ANSI_RED + "Impossibile trovare l'utente." + ANSI_RESET);
    }
}