package boardinghouse.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

// a panel with a solid fill, rounded corners, and an optional 1 px border
public class RoundedPanel extends JPanel {
    private Color fill;
    private Color line;
    private final int radius;

    public RoundedPanel(Color fill, Color line, int radius, int padding) {
        this.fill = fill;
        this.line = line;
        this.radius = radius;
        setOpaque(false);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    public void setFill(Color c) {
        fill = c;
        repaint();
    }

    public void setLine(Color c) {
        line = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float w = getWidth();
        float h = getHeight();
        float arc = radius * 2f;

        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

        // the border is drawn half a pixel inside, so no part of it is cut off at the edges
        if (line != null) {
            g2.setColor(line);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc - 1f, arc - 1f));
        }
        g2.dispose();
        super.paintComponent(g);
    }
}