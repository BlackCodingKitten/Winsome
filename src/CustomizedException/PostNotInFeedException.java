package CustomizedException;

import Color.*;

public class PostNotInFeedException extends Throwable {
    public PostNotInFeedException() {
        super(ColoredText.ANSI_RED + "Il Post corrispondente all'id non è nel feed" + ColoredText.ANSI_RESET);
    }
}