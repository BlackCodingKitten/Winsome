package CustomizedException;

import Color.*;

public class SameUserException extends Throwable {
    public SameUserException() {
        super(ColoredText.ANSI_RED + "Stai cercando di votare o commentare te stesso? Cattivo utente!!!"
                + ColoredText.ANSI_RESET);
    }
}