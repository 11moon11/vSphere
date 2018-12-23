package gui.MainWindow;

import com.vmware.vim25.mo.*;
import gui.GuiHelper;
import vSphere.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

//TODO: buffer file structure locally (in a file / DB) to speed up the process

/**
 * Creates a tree  containing file structure of the vSphere (Runnable)
 */
public class JTreeCreator implements Runnable {
    private DefaultMutableTreeNode root;
    DefaultTreeModel defaultTreeModel;
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

                //System.out.println("Child: " + childItems[i].getName() + " : " + childItems[i].getMOR().type);

                // In case we want to sort it (slow)
                //Arrays.sort(childItems, (o1, o2) -> o1.getName().compareTo(o2.getName()));

                if(childItems[0].getMOR().type.equals("Folder")) {
                    DefaultMutableTreeNode nextRoot = new DefaultMutableTreeNode(new ManagedEntityWrapper(childItems[i]));
                    MainGUI.getInsatnce().setStatusMessage("Working on: " + path + Converter.convert(childItems[i]).getName());

                    recFolderTraverse(nextRoot, Converter.convert(childItems[i]), path);
                    fRoot.add(nextRoot);
                } else {
                    fRoot.add(new DefaultMutableTreeNode(new ManagedEntityWrapper(childItems[i])));
                }
            }
            // Call sort on current root
            Thread thread = new Thread(new sortObjects(fRoot));
            thread.start();
        } catch (Exception ex) {
            GuiHelper.messageBox(ex.toString(), "Failed to traverse directories", true);
        }
    }

    public JTreeCreator(Folder rootFolder, DefaultMutableTreeNode root, DefaultTreeModel defaultTreeModel) {
        this.defaultTreeModel = defaultTreeModel;
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
            recFolderTraverse(root, rootFolder, "");
        } catch (Exception ex) {
            GuiHelper.messageBox(ex.toString(), "Failed to update directory structure", true);
            ex.printStackTrace();
        }

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

            System.out.println("Child count: " + len);

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

    private class Indexer implements Comparable<Indexer> {
        private String name;
        private int index;

        public Indexer(String name, int index) {
            this.index = index;
            this.name = name;
        }

        public String toString() {
            return name;
        }

        @Override
        public int compareTo(Indexer o) {
            return this.toString().compareToIgnoreCase(o.toString());
        }
    }
}
