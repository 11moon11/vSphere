package gui.LoginWindow;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import gui.*;
import vSphere.*;
import gui.MainWindow.*;

import javax.swing.border.TitledBorder;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

/**
 * Singleton class for the login GUI
 */
public class GUI extends JFrame {
    private static GUI insatnce;

    private vSphere vs;
    private JButton btnLogin;
    private JPanel loginForm;
    private JTextField vcentercerocTntechEduTextField;
    private JTextField textField2;
    private JPasswordField passwordField1;

    /**
     * Function used to obtain and initialize instance
     *
     * @return Login GUI class
     */
    public static GUI getInsatnce() {
        if (insatnce == null) {
            insatnce = new GUI();
        }

        return insatnce;
    }

    /**
     * Private constructor for the login GUI class
     */
    private GUI() {
        super("CEROC vCenter Helper");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(loginForm);
        pack();
        GuiHelper.centerJFrame(this);

        KeyAdapter ka = new KeyAdapter() { // Needed for the ease of use (ENTER to login)
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        };

        btnLogin.addKeyListener(ka);
        textField2.addKeyListener(ka);
        passwordField1.addKeyListener(ka);
        vcentercerocTntechEduTextField.addKeyListener(ka);

        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                login();
            }
        });
    }

    /**
     * Function that performs login functionality
     */
    private void login() {
        try {
            // Attempt connecting to the vSphere.vSphere
            vs = vSphere.getInstance(vcentercerocTntechEduTextField.getText(), textField2.getText(), new String(passwordField1.getPassword()));
            if (vs.isConnected()) { // If we successfully connect
                System.out.println("Connected");

                // Open main windows
                MainGUI.getInsatnce().setVisible(true);
                // Hide login window
                setVisible(false);
                // Clear password field
                passwordField1.setText("");
            }
        } catch (Exception ex) { // In case something goes wrong
            ex.printStackTrace();
            GuiHelper.messageBox(ex.toString(), "Connection failed!", true);
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
        loginForm = new JPanel();
        loginForm.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        Font loginFormFont = this.$$$getFont$$$(null, -1, 16, loginForm.getFont());
        if (loginFormFont != null) loginForm.setFont(loginFormFont);
        loginForm.putClientProperty("html.disable", Boolean.FALSE);
        loginForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "CEROC vCenter Helper", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, this.$$$getFont$$$(null, -1, -1, loginForm.getFont())));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font panel1Font = this.$$$getFont$$$(null, -1, 16, panel1.getFont());
        if (panel1Font != null) panel1.setFont(panel1Font);
        loginForm.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 16, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("vCenter url:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, -1, 16, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText("User name:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, -1, 16, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("Password:");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLogin = new JButton();
        Font btnLoginFont = this.$$$getFont$$$(null, -1, 16, btnLogin.getFont());
        if (btnLoginFont != null) btnLogin.setFont(btnLoginFont);
        btnLogin.setText("Login");
        loginForm.add(btnLogin, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font panel2Font = this.$$$getFont$$$(null, -1, 16, panel2.getFont());
        if (panel2Font != null) panel2.setFont(panel2Font);
        loginForm.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        vcentercerocTntechEduTextField = new JTextField();
        Font vcentercerocTntechEduTextFieldFont = this.$$$getFont$$$(null, -1, 16, vcentercerocTntechEduTextField.getFont());
        if (vcentercerocTntechEduTextFieldFont != null)
            vcentercerocTntechEduTextField.setFont(vcentercerocTntechEduTextFieldFont);
        vcentercerocTntechEduTextField.setText("vcenterceroc.tntech.edu");
        panel2.add(vcentercerocTntechEduTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null, 0, false));
        textField2 = new JTextField();
        Font textField2Font = this.$$$getFont$$$(null, -1, 16, textField2.getFont());
        if (textField2Font != null) textField2.setFont(textField2Font);
        panel2.add(textField2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null, 0, false));
        passwordField1 = new JPasswordField();
        Font passwordField1Font = this.$$$getFont$$$(null, -1, 16, passwordField1.getFont());
        if (passwordField1Font != null) passwordField1.setFont(passwordField1Font);
        panel2.add(passwordField1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null, 0, false));
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
        return loginForm;
    }
}
