package gui;

import javax.swing.*;
import java.awt.*;

public class GuiHelper {
    public static void messageBox(String msg, String title, boolean error) {
        int option = JOptionPane.INFORMATION_MESSAGE;
        if (error) {
            option = JOptionPane.ERROR_MESSAGE;
        }
        JOptionPane.showMessageDialog(null, msg, title, option);
    }

    public static void centerJFrame(JFrame frame) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
    }
}
