package CustomizedException;
public class OperationNotFoundException extends Throwable {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public OperationNotFoundException() {
        super();
    }
}