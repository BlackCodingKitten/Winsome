import java.rmi.*;

public interface RmiWinsomeServerInterface extends Remote {
    public String register(String nickname, String password, String[] tags) throws RemoteException;
}