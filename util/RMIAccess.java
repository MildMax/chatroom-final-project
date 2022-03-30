package util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIAccess<K> {

    private String interfaceName;
    private String hostname;
    private int port;

    public RMIAccess(String hostname, int port, String interfaceName) {
        this.hostname = hostname;
        this.port = port;
        this.interfaceName = interfaceName;
    }

    public K getAccess() throws RemoteException, NotBoundException {
        K access;
        try {
            Registry registry = LocateRegistry.getRegistry(InetAddress.getByName(hostname).getHostAddress(), port);
            access = (K) registry.lookup(interfaceName);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Unable to resolve host at \"%s\"",
                    this.hostname
            ));
        } catch (RemoteException e) {
            throw new RemoteException(ThreadSafeStringFormatter.format(
                    "Error occurred during remote communication: %s",
                    e.getMessage()
            ));
        } catch (NotBoundException e) {
            throw new NotBoundException(ThreadSafeStringFormatter.format(
                    "Error occurred when looking up registry for \"%s\" at \"%s:%d\": %s",
                    this.interfaceName,
                    this.hostname,
                    this.port,
                    e.getMessage()
            ));
        }

        return access;
    }

    public String getHostname() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

}
