package boardinghouse.gui;

import javax.swing.*;
import java.awt.*;

// a summary card: label, big number, small note, and a colored icon
public class StatCard extends RoundedPanel {
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = Theme.muted(" ");

    public StatCard(String label, String icon, Color tint) {
        super(Theme.CARD, Theme.BORDER, 12, 16);
        setLayout(new BorderLayout(0, 4));
        setPreferredSize(new Dimension(180, 112));

        JLabel title = new JLabel(label);
        title.setFont(Theme.font(10, Font.PLAIN));
        title.setForeground(Theme.MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(new IconTile(icon, tint), BorderLayout.EAST);

        valueLabel.setFont(Theme.font(18, Font.BOLD));

        add(top, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(noteLabel, BorderLayout.SOUTH);
    }

    public void setValue(String text) {
        // long amounts use a smaller font so they still fit
        valueLabel.setFont(Theme.font(text.length() > 11 ? 14 : 18, Font.BOLD));
        valueLabel.setText(text);
    }

    public void setNote(String text, Color color) {
        noteLabel.setText(text);
        noteLabel.setForeground(color);
    }

    private static class IconTile extends JComponent {
        private final String icon;
        private final Color tint;

        IconTile(String icon, Color tint) {
            this.icon = icon;
            this.tint = tint;
            setPreferredSize(new Dimension(36, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 40));
            g2.fillRoundRect(0, 0, 36, 36, 10, 10);
            Icons.draw(g2, icon, 9, 9, 18, tint);
            g2.dispose();
        }
    }
}