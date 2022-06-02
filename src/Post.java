import java.util.Date;
import java.time.LocalDateTime;

public class Post {
    private final int postId;
    private final String owner;
    private final String title;
    private final String text;
    private final Date date;

    public Post() {
        this.date = new Date();
    }

}