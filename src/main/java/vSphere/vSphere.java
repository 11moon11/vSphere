package vSphere;

import com.vmware.vim25.*;
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
    private HostSystem hs;

    private Folder rootFolder;
    private boolean connected;

    public static HashMap<String, ManagedEntityWrapper> VirtualMachinesMewMap, FolderMewMap, NetworkMewMap;
    public static HashMap<String, VirtualMachine> VirtualMachinesMap;
    public static HashMap<String, String> PortGroupMap;
    public static HashMap<String, Folder> FolderMap;

    private ArrayList<ResourcePool> resourcePools;

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

        // Dereference password from memory for security reasons
        password = "";
        password = null;

        rootFolder = si.getRootFolder();
        System.out.println("Root folder: " + rootFolder.getName());

        resourcePools = new ArrayList<>();
        ManagedEntity[] rps = new InventoryNavigator(rootFolder).searchManagedEntities("ResourcePool");
        for (ManagedEntity rp : rps) {
            resourcePools.add((ResourcePool) rp);
        }

        ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
        if(hosts != null && hosts.length > 0) {
            hs = (HostSystem) hosts[0];
        }

        DistributedVirtualPortgroup dvp = searchForPortgroup("dvPG-CIG-Blue-Train01-LAN");

        System.out.print("Indexing Networks...");
        NetworkMewMap = new HashMap<>();
        for(Network networkName : hs.getNetworks()) {
            String key = networkName.toString();
            NetworkMewMap.put(key, new ManagedEntityWrapper(networkName));
        }
        System.out.println("Done!");

        System.out.print("Indexing Folders...");
        FolderMewMap = new HashMap<>();
        FolderMap = new HashMap<>();
        ManagedEntity[] foldersMe = new InventoryNavigator(rootFolder).searchManagedEntities("Folder");
        for(ManagedEntity folderMe : foldersMe) {
            String key = folderMe.toString();
            FolderMewMap.put(key, new ManagedEntityWrapper(folderMe));
            FolderMap.put(key, (Folder)folderMe);
        }
        System.out.println("Done!");

        System.out.print("Indexing VMs...");
        VirtualMachinesMewMap = new HashMap<>();
        VirtualMachinesMap = new HashMap<>();
        ManagedEntity[] vmsMe = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        for(ManagedEntity vmMe : vmsMe) {
            String key = vmMe.toString();
            VirtualMachinesMewMap.put(key, new ManagedEntityWrapper(vmMe));
            VirtualMachinesMap.put(key, (VirtualMachine)vmMe);
        }
        System.out.println("Done!");

        if (si != null) {
            this.connected = true;
        } else {
            this.connected = false;
        }
    }

    public DistributedVirtualPortgroup searchForPortgroup(String name) throws RemoteException {
        for (ManagedEntity dc : new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter")) {
            ManagedEntity tmp = new InventoryNavigator(((Datacenter) dc).getNetworkFolder()).searchManagedEntity("DistributedVirtualPortgroup", name);
            if(tmp != null) return (DistributedVirtualPortgroup) tmp;
        }

        return null;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    public ArrayList<ResourcePool> getResourcePools() {
        return resourcePools;
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

        for(ManagedEntity me : new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter")) {
            if(me != null) {
                Datacenter dc = (Datacenter) me;

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

    public HostSystem getHostSystem() {
        return hs;
    }
}
