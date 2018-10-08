package gui.MainWindow;

import com.intellij.uiDesigner.core.Spacer;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import com.vmware.vim25.mo.SearchIndex;
import gui.*;
import vSphere.*;
import gui.LoginWindow.*;
import gui.MainWindow.Functionality.*;

import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.rmi.*;
import java.util.concurrent.TimeUnit;


public class MainGUI extends JFrame {
    private static MainGUI insatnce;

    private DefaultMutableTreeNode fileStructure;
    private ManagedEntityWrapper selectedItem;
    private DefaultTreeModel defaultTreeModel;
    private ArrayList<Thread> threads;
    private JButton refreshButton;
    private JTree fileExplorer;

    private JPanel panel1;
    private JPanel description;
    private JPanel details;
    private JLabel file_name;
    private JLabel file_type;
    private JLabel file_location;
    private JTextField status;
    private JPanel functionality;
    private vSphere vs;

    public static MainGUI getInsatnce() {
        if (insatnce == null) {
            insatnce = new MainGUI();
        }

        // Will only execute after login
        if (!insatnce.isVisible()) {
            insatnce.init();
        }

        return insatnce;
    }

    //TODO: add images for VMs, Templates and Folders
    private void init() {
        vs = vSphere.getInstance();
        GuiHelper.centerJFrame(this);

        //TODO: replace functionality panel depending on what item is selected
        functionality.add(FolderWindow.getInstance().getPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        update();

        if (vs == null) {
            GuiHelper.messageBox("Failed to obtain vSphere.vSphere instance", "Initialization Error", false);
        }
    }

    private MainGUI() {
        super("CEROC vCenter Helper");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setContentPane(panel1);
        pack();

        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                refreshButton.setEnabled(false);
                fileExplorer.setEnabled(false);
                update();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                super.componentHidden(e);
                if (vs != null) {
                    vs.disconnect();
                }
                GUI.getInsatnce().setVisible(true);
            }
        });

        fileExplorer.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileExplorer.getLastSelectedPathComponent();
                if (selectedNode == null) return;
                selectedItem = ((ManagedEntityWrapper) selectedNode.getUserObject());
                if (selectedItem == null) return;

                file_name.setText("File name: " + selectedItem.getName());
                file_type.setText("File type: " + selectedItem.getType());
                file_location.setText("File location: " + e.getPath().toString());

                JPanel panel = null;
                if (selectedItem.type() == ManagedEntityWrapper.Type.FOLDER) {
                    panel = FolderWindow.getInstance().getPanel();
                } else if (selectedItem.type() == ManagedEntityWrapper.Type.VM) {
                    panel = VMWindow.getInstance().getPanel(selectedItem);
                } else if (selectedItem.type() == ManagedEntityWrapper.Type.TEMPLATE) {
                    panel = TemplateWindow.getInstance().getPanel();
                }

                if (panel == null) return;

                functionality.remove(0);
                functionality.add(panel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
            }
        });
    }

    public ManagedEntityWrapper getSelectedItem() {
        return selectedItem;
    }

    public void setStatusMessage(String text) {
        status.setText(text);
    }

    /**
     * Updates file structure
     */
    private void update() {
        fileStructure = new DefaultMutableTreeNode(new ManagedEntityWrapper(vs.getRootFolder()));
        defaultTreeModel = new DefaultTreeModel(fileStructure);
        fileExplorer.setModel(defaultTreeModel);

        setStatusMessage("Status: updating file structure...");
        if (vs != null) {
            try {
                threads = vs.getFiles(fileStructure, defaultTreeModel);
                new Thread(new WaitUntilDone()).start();
            } catch (RemoteException ex) {
                GuiHelper.messageBox(ex.toString(), "Refresh failed!", true);
            }
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        Font panel1Font = this.$$$getFont$$$(null, -1, 14, panel1.getFont());
        if (panel1Font != null) panel1.setFont(panel1Font);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, 14, panel1.getFont())));
        final JScrollPane scrollPane1 = new JScrollPane();
        Font scrollPane1Font = this.$$$getFont$$$(null, -1, 14, scrollPane1.getFont());
        if (scrollPane1Font != null) scrollPane1.setFont(scrollPane1Font);
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(450, -1), null, null, 0, false));
        fileExplorer = new JTree();
        fileExplorer.setDropMode(DropMode.ON);
        fileExplorer.setEditable(false);
        fileExplorer.setEnabled(true);
        Font fileExplorerFont = this.$$$getFont$$$(null, -1, 14, fileExplorer.getFont());
        if (fileExplorerFont != null) fileExplorer.setFont(fileExplorerFont);
        fileExplorer.setRootVisible(true);
        fileExplorer.setShowsRootHandles(false);
        fileExplorer.putClientProperty("JTree.lineStyle", "");
        scrollPane1.setViewportView(fileExplorer);
        refreshButton = new JButton();
        Font refreshButtonFont = this.$$$getFont$$$(null, -1, 14, refreshButton.getFont());
        if (refreshButtonFont != null) refreshButton.setFont(refreshButtonFont);
        refreshButton.setIcon(new ImageIcon(getClass().getResource("/gtk_refresh.png")));
        refreshButton.setText("");
        panel1.add(refreshButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        description = new JPanel();
        description.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font descriptionFont = this.$$$getFont$$$(null, -1, 14, description.getFont());
        if (descriptionFont != null) description.setFont(descriptionFont);
        description.setToolTipText("");
        panel1.add(description, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(650, -1), null, null, 0, false));
        description.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Properties & Actions", TitledBorder.LEFT, TitledBorder.TOP, this.$$$getFont$$$(null, -1, 14, description.getFont()), new Color(-16777216)));
        details = new JPanel();
        details.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font detailsFont = this.$$$getFont$$$(null, -1, 14, details.getFont());
        if (detailsFont != null) details.setFont(detailsFont);
        description.add(details, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        details.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Details:"));
        file_name = new JLabel();
        Font file_nameFont = this.$$$getFont$$$(null, -1, 14, file_name.getFont());
        if (file_nameFont != null) file_name.setFont(file_nameFont);
        file_name.setText("File name:");
        details.add(file_name, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        file_type = new JLabel();
        Font file_typeFont = this.$$$getFont$$$(null, -1, 14, file_type.getFont());
        if (file_typeFont != null) file_type.setFont(file_typeFont);
        file_type.setText("File type:");
        details.add(file_type, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        file_location = new JLabel();
        Font file_locationFont = this.$$$getFont$$$(null, -1, 14, file_location.getFont());
        if (file_locationFont != null) file_location.setFont(file_locationFont);
        file_location.setText("File location:");
        details.add(file_location, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        functionality = new JPanel();
        functionality.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font functionalityFont = this.$$$getFont$$$(null, -1, 14, functionality.getFont());
        if (functionalityFont != null) functionality.setFont(functionalityFont);
        description.add(functionality, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        functionality.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Additional functionality:"));
        final Spacer spacer1 = new Spacer();
        description.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        description.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        status = new JTextField();
        status.setEditable(false);
        status.setEnabled(true);
        status.setText("Status:");
        panel1.add(status, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    /**
     * Utility class that disables refresh button while refresh is in progress
     * Works is a separate thread
     */
    private class WaitUntilDone implements Runnable {
        @Override
        public void run() {
            // Disable button
            refreshButton.setEnabled(false);
            fileExplorer.setEnabled(false);

            for (Thread th : threads) {
                try {
                    th.join();
                } catch (InterruptedException e) {
                    GuiHelper.messageBox(e.toString(), "Refresh failed!", true);
                    setStatusMessage("Status: failed to updating file structure!");
                }
            }

            // Update tree view
            defaultTreeModel.reload();
            //fileExplorer.updateUI();
            status.setText("Status: file structure updated!");

            // Enable button
            refreshButton.setEnabled(true);
            fileExplorer.setEnabled(true);
        }

        /**
         * Constructor for the Runnable WaitUntilDone class
         */
        public WaitUntilDone() {
        }
    }

}
