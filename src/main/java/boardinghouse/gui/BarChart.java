package boardinghouse.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

// simple bar chart for the monthly income (hover a bar to see the amount)
public class BarChart extends JComponent {
    private static final int LEFT = 46;
    private static final int RIGHT = 8;
    private static final int TOP = 10;
    private static final int BOTTOM = 26;

    private String[] labels = new String[0];
    private double[] values = new double[0];

    public BarChart() {
        setPreferredSize(new Dimension(300, 240));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setData(String[] labels, double[] values) {
        this.labels = labels;
        this.values = values;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (values.length == 0) return null;
        double slot = (getWidth() - LEFT - RIGHT) / (double) values.length;
        int i = (int) Math.floor((e.getX() - LEFT) / slot);
        if (i < 0 || i >= values.length) return null;
        return labels[i] + ": " + Ui.peso(values[i]);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cw = w - LEFT - RIGHT;
        int ch = h - TOP - BOTTOM;
        if (cw <= 0 || ch <= 0 || values.length == 0) {
            g2.dispose();
            return;
        }

        double max = 0;
        for (double v : values) max = Math.max(max, v);
        double step = niceStep(max);
        double topValue = step * 4;

        g2.setFont(Theme.font(9, Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();

        // grid lines and the numbers on the left
        for (int i = 0; i <= 4; i++) {
            int y = TOP + ch - (int) Math.round(ch * i / 4.0);
            g2.setColor(i == 0 ? Theme.BORDER_STRONG : new Color(0x26, 0x34, 0x49, 140));
            g2.drawLine(LEFT, y, LEFT + cw, y);
            String text = axis(step * i);
            g2.setColor(Theme.MUTED);
            g2.drawString(text, LEFT - 8 - fm.stringWidth(text), y + fm.getAscent() / 2 - 1);
        }

        double slot = cw / (double) values.length;
        double barWidth = Math.min(28, slot * 0.5);

        for (int i = 0; i < values.length; i++) {
            double x = LEFT + slot * i + (slot - barWidth) / 2;

            int bh = (int) Math.round(ch * values[i] / topValue);
            if (bh > 0) {
                int by = TOP + ch - bh;
                g2.setPaint(new GradientPaint(0, by, Theme.ACCENT, 0, by + bh, Theme.ACCENT_HOVER));
                g2.fill(new RoundRectangle2D.Double(x, by, barWidth, bh, 8, 8));
                g2.fillRect((int) x, by + bh / 2, (int) barWidth, bh - bh / 2); // square bottom
            }

            g2.setColor(Theme.MUTED);
            String label = i < labels.length ? labels[i] : "";
            g2.drawString(label, (float) (x + (barWidth - fm.stringWidth(label)) / 2), h - 8);
        }

        if (max <= 0) {
            String text = "No approved payments for this year yet";
            g2.setColor(Theme.MUTED);
            g2.drawString(text, LEFT + (cw - fm.stringWidth(text)) / 2, TOP + ch / 2);
        }
        g2.dispose();
    }

    // picks a clean step (1, 2, 2.5, 5, 10 ...) so four steps cover the biggest value
    private static double niceStep(double max) {
        if (max <= 0) return 1;
        double base = Math.pow(10, Math.floor(Math.log10(max / 4)));
        double step = base;
        for (double m : new double[]{1, 2, 2.5, 5, 10}) {
            step = m * base;
            if (step * 4 >= max) break;
        }
        return step;
    }

    private static String axis(double v) {
        if (v >= 1000) {
            String s = String.format("%.1f", v / 1000.0);
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return s + "k";
        }
        if (v == Math.rint(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }
}