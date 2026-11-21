package boardinghouse.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// base class for each screen of the dashboard; every section must say how it reloads its data
public abstract class Section extends JPanel implements Refreshable {
    protected Section() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 24, 24, 24));
    }

    @Override
    public abstract void refresh();

    protected void msg(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}