package components;

import java.awt.Font;

import javax.swing.JTextArea;

public class CustomTextArea extends JTextArea {
    public CustomTextArea(String text){
        super(text);

        setEditable(false);

        setLineWrap(false);

        setWrapStyleWord(false);

        setFont(new Font("Segoe UI", Font.PLAIN, 15));

    }
}
