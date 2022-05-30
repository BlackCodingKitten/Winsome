import java.rmi.*;

public interface RMIWinsomeServerInterface extends Remote {
    public String register(String nickname, String password, String[] tags) throws RemoteException;
}