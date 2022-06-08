import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RmiServiceInterface extends Remote {
    boolean registerNewUser(String nickname, String password, ArrayList<String> tags) throws RemoteException;

    ArrayList<String> followerList(String nickname, String password) throws RemoteException;
}
