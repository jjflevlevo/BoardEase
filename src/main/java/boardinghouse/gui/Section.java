package boardinghouse.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// base class for each section of the owner dashboard
public abstract class Section extends JPanel {
    protected Section() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 24, 24, 24));
    }

    public abstract void refresh();

    protected void msg(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}