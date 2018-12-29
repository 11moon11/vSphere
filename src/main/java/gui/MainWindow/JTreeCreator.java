package gui.MainWindow;

import com.vmware.vim25.mo.*;
import gui.GuiHelper;
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
    private ArrayList<ManagedEntityWrapper> notNetworkedVMs;
    private DefaultTreeModel defaultTreeModel;
    private DefaultMutableTreeNode root;
    private Folder rootFolder;
    private boolean done;

    private void recFolderTraverse(DefaultMutableTreeNode fRoot, Folder rootFolder, String path) throws NullPointerException {
        if(fRoot == null) {
            if(rootFolder == null) {
                throw new NullPointerException("Folder is not specified");
            }
            fRoot = new DefaultMutableTreeNode(new ManagedEntityWrapper(rootFolder));
        }

        //System.out.println("Parent: " + rootFolder.getName());
        path += rootFolder.getName() + " -> ";

        try {
            // Get child items
            ManagedEntity[] childItems = rootFolder.getChildEntity();

            for(int i=0; i<childItems.length; i++) {
                if(childItems[i] == null)
                    continue;

                if(childItems[0].getMOR().type.equals("Folder")) {
                    DefaultMutableTreeNode nextRoot = new DefaultMutableTreeNode(new ManagedEntityWrapper(childItems[i]));
                    MainGUI.getInsatnce().setStatusMessage("Working on: " + path + Converter.convert(childItems[i]).getName());

                    recFolderTraverse(nextRoot, Converter.convert(childItems[i]), path);
                    fRoot.add(nextRoot);
                } else {
                    // Populate folder view
                    ManagedEntityWrapper mewChild = new ManagedEntityWrapper(childItems[i]);
                    fRoot.add(new DefaultMutableTreeNode(mewChild));

                    // Generate network view
                    if(mewChild.getFirstNetwork() == null) {
                        notNetworkedVMs.add(mewChild);
                    }
                }
            }
            // Call sort on current root
            Thread thread = new Thread(new sortObjects(fRoot));
            thread.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            GuiHelper.messageBox(ex.toString(), "Failed to traverse directories", true);
        }
    }

    private void recFolderTraverse2(DefaultMutableTreeNode fRoot, Folder rootFolder, String path) {
        HashMap<String, DefaultMutableTreeNode> parentMap = new HashMap<>();
        Queue<ManagedEntityWrapper> foldersToExplore = new LinkedList<>();
        foldersToExplore.add(vSphere.FolderMewMap.get(rootFolder.toString()));
        parentMap.put(foldersToExplore.peek().meKey, fRoot);

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
                    fRoot.add(new DefaultMutableTreeNode(folderChildMew));
                }
            }
        }
    }

    public JTreeCreator(Folder rootFolder, DefaultMutableTreeNode root, DefaultTreeModel defaultTreeModel) {
        this.defaultTreeModel = defaultTreeModel;
        this.notNetworkedVMs = new ArrayList<>();
        this.rootFolder = rootFolder;
        this.root = root;

        root.removeAllChildren();
    }

    public DefaultMutableTreeNode getFolders() {
        return root;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public void run() {
        done = false;
        try { // Obtain directory structure
            recFolderTraverse2(root, rootFolder, "");
        } catch (Exception ex) {
            GuiHelper.messageBox(ex.toString(), "Failed to updateTree directory structure", true);
            ex.printStackTrace();
        }

        /* TODO: rewrite
        // Populate network view
        HostSystem hs = vSphere.getInstance().getHostSystem();
        DefaultMutableTreeNode networkStructure = new DefaultMutableTreeNode(new ManagedEntityWrapper(hs));
        DefaultTreeModel defaultTreeModelNetwork = new DefaultTreeModel(networkStructure);
        DefaultListModel<ManagedEntityWrapper> notNetworkedVMsList = new DefaultListModel<>();
        try {
            for(Network networkName : hs.getNetworks()) { // Populate networked vms tree
                DefaultMutableTreeNode parentNetwork = new DefaultMutableTreeNode(new ManagedEntityWrapper(networkName));
                networkStructure.add(parentNetwork);
                for(VirtualMachine vm : networkName.getVms()) {
                    parentNetwork.add(new DefaultMutableTreeNode(new ManagedEntityWrapper(vm)));
                }
            }

            Thread thread = new Thread(new sortObjects(networkStructure));
            thread.start();

            for(ManagedEntityWrapper vmMew : notNetworkedVMs) { // Populate not networked vms list
                VirtualMachine vm = vSphere.VirtualMachinesMap.get(vmMew.meKey);
                notNetworkedVMsList.add(notNetworkedVMsList.getSize(), new ManagedEntityWrapper(vm, true));
            }
        } catch (RemoteException e) {
            //GuiHelper.messageBox(e.toString(), "Network view refresh failed!", true);
            e.printStackTrace();
        }
        MainGUI.getInsatnce().updateNetworkViewCallback(defaultTreeModelNetwork);
        MainGUI.getInsatnce().updateNotNetworkedViewCallback(notNetworkedVMsList);
        */

        done = true;
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
