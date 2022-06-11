package CustomizedException;

public class PostLengthException extends Throwable {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public PostLengthException() {
        super(ANSI_RED + "Testo o titolo del post troppo lunghi.\n" + ANSI_RESET);
    }
}
