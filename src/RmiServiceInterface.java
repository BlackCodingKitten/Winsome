import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

public interface RmiServiceInterface extends Remote {
    boolean registerNewUser(String nickname, String password, Set<String> tags) throws RemoteException;

    Set<String> followerList(String nickname, String password) throws RemoteException;
}
