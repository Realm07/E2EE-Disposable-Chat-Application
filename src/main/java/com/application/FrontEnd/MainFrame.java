
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.*;;

public class MainFrame extends JFrame{
    private LoginPage loginPage;
    private ChatRoom chatRoom;
    
    public MainFrame(){
        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Image icon = Toolkit.getDefaultToolkit().getImage("src/images/Chat_logo.png");
        setIconImage(icon);
        setBounds(100, 100, 850, 550);
        loginPage  = new LoginPage(this);
        add(loginPage);
        setVisible(true);
        revalidate(); 
        repaint();
    }

    public void switchToChatRoom(String CurrentUserName, String CurrentRoomName){
        remove(loginPage);
        chatRoom = new ChatRoom(CurrentUserName, CurrentRoomName, this);
        add(chatRoom);
        revalidate(); 
        repaint();
    }
    public void switchToLoginPage(){
        remove(chatRoom);
        loginPage = new LoginPage(this);
        add(loginPage);
        revalidate(); 
        repaint();
    }

    public static void main(String[] args) {
        new MainFrame();
    }
}
