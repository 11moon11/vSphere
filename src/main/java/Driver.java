import gui.LoginWindow.GUI;

public class Driver {
    public static void main(String[] args) {
        try {
            GUI gui = GUI.getInsatnce();
            gui.setVisible(true);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
