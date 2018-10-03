package gui.MainWindow;

import com.vmware.vim25.mo.*;
import gui.GuiHelper;
import sun.applet.Main;
import vSphere.*;

import javax.swing.*;
import javax.swing.tree.*;

/**
 * Creates a tree  containing file structure of the vSphere (Runnable)
 */
public class JTreeCreator implements Runnable {
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
            for (int i=0; i<childItems.length; i++) {
                if(childItems[i] == null)
                    continue;

                //System.out.println("Child: " + childItems[i].getName() + " : " + childItems[0].getMOR().type);

                if(childItems[0].getMOR().type.equals("Folder")) {
                    DefaultMutableTreeNode nextRoot = new DefaultMutableTreeNode(new ManagedEntityWrapper(childItems[i]));

                    MainGUI.getInsatnce().setStatusMessage("Working on: " + path + Converter.convert(childItems[i]).getName());

                    recFolderTraverse(nextRoot, Converter.convert(childItems[i]), path);
                    fRoot.add(nextRoot);
                } else {
                    fRoot.add(new DefaultMutableTreeNode(new ManagedEntityWrapper(childItems[i])));
                }
            }
        } catch (Exception ex) {
            GuiHelper.messageBox(ex.toString(), "Failed to traverse directories", true);
        }
    }

    public JTreeCreator(Folder rootFolder, DefaultMutableTreeNode root) {
        this.rootFolder = rootFolder;
        this.root = root;
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
        recFolderTraverse(root, rootFolder, "");
        done = true;
    }
}
