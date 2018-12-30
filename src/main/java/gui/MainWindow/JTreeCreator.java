package gui.MainWindow;

import com.vmware.vim25.mo.*;
import vSphere.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.rmi.RemoteException;
import java.util.*;

//TODO: buffer file structure locally (in a file / DB) to speed up the process

/**
 * Creates a tree  containing file structure of the vSphere (Runnable)
 */
public class JTreeCreator implements Runnable {
    private HashSet<ManagedEntityWrapper> notNetworkedVMs;
    private DefaultTreeModel defaultTreeModel;
    private DefaultMutableTreeNode root;
    private boolean done, full;
    private Folder rootFolder;

    private void folderTraverse(DefaultMutableTreeNode fRoot, Folder rootFolder, String path) {
        HashMap<String, DefaultMutableTreeNode> parentMap = new HashMap<>();
        Queue<ManagedEntityWrapper> foldersToExplore = new LinkedList<>();
        foldersToExplore.add(vSphere.FolderMewMap.get(rootFolder.toString()));
        parentMap.put(foldersToExplore.peek().meKey, fRoot);
        notNetworkedVMs.clear();

        while(foldersToExplore.size() > 0) {
            ManagedEntityWrapper folderInProgress = foldersToExplore.poll();
            fRoot = new DefaultMutableTreeNode(folderInProgress);
            if(parentMap.containsKey(folderInProgress.meKey))
                parentMap.get(folderInProgress.meKey).add(fRoot);

            ManagedEntityWrapper[] folderChildrenMew = folderInProgress.getFolderChildren();
            Arrays.sort(folderChildrenMew);
            for(ManagedEntityWrapper folderChildMew : folderChildrenMew) {
                parentMap.put(folderChildMew.meKey, fRoot);
                if(folderChildMew.type() == ManagedEntityWrapper.Type.FOLDER) {
                    foldersToExplore.add(folderChildMew);
                } else {
                    // Populate folder view
                    fRoot.add(new DefaultMutableTreeNode(folderChildMew));
                    // Generate network view
                    if(!folderChildMew.isNetworked) {
                        notNetworkedVMs.add(folderChildMew);
                    }
                }
            }
        }
    }

    public JTreeCreator(Folder rootFolder, DefaultMutableTreeNode root, DefaultTreeModel defaultTreeModel) {
        this.defaultTreeModel = defaultTreeModel;
        this.notNetworkedVMs = new HashSet<>();
        this.rootFolder = rootFolder;
        this.root = root;
        this.full = true;

        root.removeAllChildren();
    }

    public JTreeCreator(Folder rootFolder, DefaultMutableTreeNode root, DefaultTreeModel defaultTreeModel, boolean full) {
        this.defaultTreeModel = defaultTreeModel;
        this.notNetworkedVMs = new HashSet<>();
        this.rootFolder = rootFolder;
        this.root = root;
        this.full = full;

        root.removeAllChildren();
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public void run() {
        done = false;
        MainGUI.getInsatnce().setStatusMessage("Status: Updating folder structure...");
        folderTraverse(root, rootFolder, "");
        done = true;

        if(full)
            new Thread(new PopulateNetworkView(notNetworkedVMs)).start();
    }

    private class PopulateNetworkView implements Runnable {
        private HashSet<ManagedEntityWrapper> notNetworkedVMs;

        PopulateNetworkView(HashSet<ManagedEntityWrapper> notNetworkedVMs) {
            this.notNetworkedVMs = notNetworkedVMs;
        }

        @Override
        public void run() {
            // Populate not networked vms list
            MainGUI.getInsatnce().setStatusMessage("Status: Updating list of not networked devices...");
            ManagedEntityWrapper []notNetworkedArr = notNetworkedVMs.toArray(new ManagedEntityWrapper[0]);
            Arrays.sort(notNetworkedArr);
            DefaultListModel<ManagedEntityWrapper> notNetworkedVMsList = new DefaultListModel<>();
            for(ManagedEntityWrapper vmMew : notNetworkedArr) { // Populate not networked vms list
                notNetworkedVMsList.add(notNetworkedVMsList.getSize(), vmMew.duplicate(true));
            }
            MainGUI.getInsatnce().updateNotNetworkedViewCallback(notNetworkedVMsList);

            // Populate networked vms tree
            MainGUI.getInsatnce().setStatusMessage("Status: Updating tree of networked devices (this one takes a minute)...");
            HostSystem hs = vSphere.getInstance().getHostSystem();
            DefaultMutableTreeNode networkStructure = new DefaultMutableTreeNode(new ManagedEntityWrapper(hs));
            DefaultTreeModel defaultTreeModelNetwork = new DefaultTreeModel(networkStructure);
            try {
                for(Network networkName : hs.getNetworks()) {
                    DefaultMutableTreeNode parentNetwork = new DefaultMutableTreeNode(new ManagedEntityWrapper(networkName));
                    networkStructure.add(parentNetwork);
                    for(VirtualMachine vm : networkName.getVms()) { //TODO: optimize
                        parentNetwork.add(new DefaultMutableTreeNode(vSphere.VirtualMachinesMewMap.get(vm.toString())));
                    }
                }
                new sortObjects(networkStructure).run();
            } catch (RemoteException e) {
                //GuiHelper.messageBox(e.toString(), "Network view refresh failed!", true);
                e.printStackTrace();
            }
            MainGUI.getInsatnce().updateNetworkViewCallback(defaultTreeModelNetwork);
        }
    }

    private class sortObjects implements Runnable {
        private DefaultMutableTreeNode objectNode;

        sortObjects(DefaultMutableTreeNode objectNode) {
            System.out.println("Sorting " + objectNode.getUserObject().toString());
            this.objectNode = objectNode;
        }

        @Override
        public void run() {
            int len = objectNode.getChildCount();
            ArrayList<DefaultMutableTreeNode> tn = new ArrayList<>();

            //System.out.println("Child count: " + len);

            for(int i=0; i<len; i++) {
                tn.add((DefaultMutableTreeNode)objectNode.getChildAt(i));
            }

            tn.sort(Comparator.comparing(o -> o.getUserObject().toString()));

            for(int i=0; i<len; i++) {
                objectNode.insert(tn.get(i), i);
            }

            defaultTreeModel.reload(objectNode);
        }
    }
}
