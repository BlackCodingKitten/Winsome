import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
;

public class WinsomeServerMain {
    public static ServerSocket serverSocket;
    public static final SocialManager socialManager;
    public static final ConcurrentLinkedQueue<Socket> socketlist;

    public static final ConcurrentHashMap<String, ClientSession> clientSessionList = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        
    }
}
