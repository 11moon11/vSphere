package gui.MainWindow.Functionality;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import gui.MainWindow.MainGUI;
import vSphere.ManagedEntityWrapper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;

public class SelectTargetFolders extends JFrame {
    private ManagedEntityWrapper selectedItem;

    private JTree folderSlection;
    private JPanel panel1;
    private JButton finishButton;
    private JList<ManagedEntityWrapper> selectedFolders;
    private JButton selectFolder;

    SelectTargetFolders(ManagedEntityWrapper selectedTemplate) {
        super("Please select destination folders");
        $$$setupUI$$$();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(panel1);
        pack();

        selectedFolders.setModel(new DefaultListModel<>());
        folderSlection.setModel(MainGUI.getInsatnce().defaultTreeModel);

        finishButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);

                for (int i = 0; i < selectedFolders.getModel().getSize(); i++) {
                    try {
                        selectedTemplate.cloneVM(((DefaultListModel<ManagedEntityWrapper>) selectedFolders.getModel()).get(i));
                    } catch (RemoteException e1) {
                        System.out.println("Failed to clone selected VM");
                        e1.printStackTrace();
                    }
                }

                hideMe();
            }
        });

        selectFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);

                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderSlection.getLastSelectedPathComponent();
                if (selectedNode == null) return;
                selectedItem = ((ManagedEntityWrapper) selectedNode.getUserObject());
                if (selectedItem == null) return;

                // We can only create a vm inside a folder
                if (selectedItem.type() != ManagedEntityWrapper.Type.FOLDER) return;

                ((DefaultListModel<ManagedEntityWrapper>) selectedFolders.getModel()).add(selectedFolders.getModel().getSize(), selectedItem);
            }
        });
    }

    private void hideMe() {
        this.setVisible(false);
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
        panel1.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        Font panel1Font = this.$$$getFont$$$(null, -1, 14, panel1.getFont());
        if (panel1Font != null) panel1.setFont(panel1Font);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        finishButton = new JButton();
        Font finishButtonFont = this.$$$getFont$$$(null, -1, 14, finishButton.getFont());
        if (finishButtonFont != null) finishButton.setFont(finishButtonFont);
        finishButton.setText("Finish");
        panel1.add(finishButton, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedFolders = new JList();
        Font selectedFoldersFont = this.$$$getFont$$$(null, -1, 14, selectedFolders.getFont());
        if (selectedFoldersFont != null) selectedFolders.setFont(selectedFoldersFont);
        panel1.add(selectedFolders, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        selectFolder = new JButton();
        Font selectFolderFont = this.$$$getFont$$$(null, -1, 14, selectFolder.getFont());
        if (selectFolderFont != null) selectFolder.setFont(selectFolderFont);
        selectFolder.setText(" -> ");
        panel1.add(selectFolder, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        Font scrollPane1Font = this.$$$getFont$$$(null, -1, 14, scrollPane1.getFont());
        if (scrollPane1Font != null) scrollPane1.setFont(scrollPane1Font);
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(450, -1), null, null, 0, false));
        folderSlection = new JTree();
        folderSlection.setDropMode(DropMode.ON);
        Font folderSlectionFont = this.$$$getFont$$$(null, -1, 14, folderSlection.getFont());
        if (folderSlectionFont != null) folderSlection.setFont(folderSlectionFont);
        scrollPane1.setViewportView(folderSlection);
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

}