import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiCallbackInterface extends Remote {
    void callbackRegister(String nickname, NotifyEventInterface cInterface) throws RemoteException;

    void callbackUnregister(String nickname) throws RemoteException;

}
