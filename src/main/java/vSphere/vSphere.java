package vSphere;

import gui.MainWindow.JTreeCreator;

import com.vmware.vim25.mo.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.net.URL;
import java.util.*;

public class vSphere {
    private static vSphere instance;
    private ServiceInstance si;

    private Folder rootFolder;
    private boolean connected;

    public static vSphere getInstance() {
        return instance;
    }

    /**
     * Reconnect to the vCenter
     * @param ip IP address for the vCenter we are trying to connect to
     * @param username User name we want to connect with
     * @param password A valid password for the vCenter
     * @return vSphere instance
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public static vSphere getInstance(String ip, String username, String password) throws RemoteException, MalformedURLException {
        instance = new vSphere(ip, username, password);

        return instance;
    }

    /**
     * Attempts to connect to the vCenter
     * @param ip IP address for the vCenter we are trying to connect to
     * @param username User name we want to connect with
     * @param password A valid password for the vCenter
     * @throws RemoteException
     * @throws MalformedURLException
     */
    private vSphere(String ip, String username, String password) throws RemoteException, MalformedURLException {
        si = new ServiceInstance(new URL("https://" + ip + "/sdk"), username, password, true);
        rootFolder = si.getRootFolder();
        System.out.println("Root folder: " + rootFolder.getName());

        // Get all virtual machine and host objects
        // Holds Virtual Machine objects
        //ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        // Holds System
        //ManagedEntity[] mesHost = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");

        //SearchIndex si = new SearchIndex()

        if (si != null) {
            this.connected = true;
        } else {
            this.connected = false;
        }
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    /**
     * Checks for thread completion
     * @param threads Threads to monitor
     * @return Returns true when all threads are complete
     */
    public boolean isDone(ArrayList<JTreeCreator> threads) {
        if(threads == null) {
            return true;
        }

        for(JTreeCreator t : threads) {
            if(!t.isDone()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Acquires directory structure of the Data Center (creates 1 thread per Data Center)
     * @param datacenters DefaultMutableTreeNode that stores the directory structure
     * @param defaultTreeModel
     * @return Returns running threads
     * @throws RemoteException
     */
    public ArrayList<Thread> getFiles(DefaultMutableTreeNode datacenters, DefaultTreeModel defaultTreeModel) throws RemoteException {
        ArrayList<Thread> threads = new ArrayList<>();

        for(ManagedEntity me : rootFolder.getChildEntity()) {
            if(me != null && me.getMOR().type.equals("Datacenter")) {
                Datacenter dc = (Datacenter)me;
                Folder vmf = dc.getVmFolder();
                if(vmf == null)
                    continue;

                // Create an entry node for the Data Center
                DefaultMutableTreeNode entry = new DefaultMutableTreeNode(new ManagedEntityWrapper(me));
                // Add an entry node to main
                datacenters.add(entry);

                // Retrieve files and folders
                // Threaded because this process takes a minute :)
                JTreeCreator jtc = new JTreeCreator(dc.getVmFolder(), entry, defaultTreeModel);
                Thread thread = new Thread(jtc);
                threads.add(thread);
                thread.start();
            }
        }

        return threads;
    }

    /**
     * Check connection to the vCenter
     * @return True if connected to the vCenter
     */
    public boolean isConnected() {
        return this.connected;
    }

    public void disconnect() {
        si.getServerConnection().logout();
    }
}
