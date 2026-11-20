package boardinghouse.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// the left navigation: logo, menu items with icons and badges, user card, and logout
public class Sidebar extends JPanel {
    public record Item(String key, String label, String icon) { }

    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final Consumer<String> onSelect;

    public Sidebar(List<Item> navItems, String userName, String userSubtitle,
                   Consumer<String> onSelect, Runnable onLogout) {
        this.onSelect = onSelect;
        setBackground(Theme.SIDEBAR);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));
        setPreferredSize(new Dimension(220, 0));

        add(new Logo(160));
        add(Box.createVerticalStrut(28));
        for (Item item : navItems) {
            NavItem nav = new NavItem(item);
            items.put(item.key(), nav);
            add(nav);
            add(Box.createVerticalStrut(4));
        }
        add(Box.createVerticalGlue());
        add(userCard(userName, userSubtitle));
        add(Box.createVerticalStrut(12));

        JButton logout = Theme.secondary("Logout");
        logout.addActionListener(e -> onLogout.run());
        add(Theme.block(logout, 40));
    }

    public void select(String key) {
        for (Map.Entry<String, NavItem> e : items.entrySet()) {
            e.getValue().selected = e.getKey().equals(key);
            e.getValue().repaint();
        }
    }

    public void setBadge(String key, int count, Color color) {
        NavItem n = items.get(key);
        if (n == null) return;
        n.badge = count;
        n.badgeColor = color;
        n.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.BORDER);
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
    }

    // ---------- User card ----------
    private JPanel userCard(String name, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.ROW_SELECTED);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(Theme.TEXT);
                g2.setFont(Theme.font(10, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                String s = initials(name);
                g2.drawString(s, (36 - fm.stringWidth(s)) / 2, (36 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.font(10, Font.BOLD));
        JLabel subLabel = Theme.muted(subtitle);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(nameLabel);
        text.add(subLabel);

        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static String initials(String name) {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    // ---------- One menu item ----------
    private class NavItem extends JComponent {
        private final String key;
        private final String label;
        private final String icon;
        private boolean selected;
        private boolean hover;
        private int badge;
        private Color badgeColor = Theme.WARN;

        NavItem(Item item) {
            this.key = item.key();
            this.label = item.label();
            this.icon = item.icon();
            setPreferredSize(new Dimension(188, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    select(key);
                    onSelect.accept(key);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (selected) {
                g2.setColor(Theme.ROW_SELECTED);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            } else if (hover) {
                g2.setColor(Theme.CARD_HOVER);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            }

            Icons.draw(g2, icon, 12, (h - 18) / 2, 18, selected ? Theme.ACCENT : Theme.MUTED);

            g2.setFont(Theme.font(11, selected ? Font.BOLD : Font.PLAIN));
            g2.setColor(selected ? Theme.TEXT : Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, 42, (h - fm.getHeight()) / 2 + fm.getAscent());

            if (badge > 0) {
                String text = badge > 99 ? "99+" : String.valueOf(badge);
                g2.setFont(Theme.font(9, Font.BOLD));
                FontMetrics bf = g2.getFontMetrics();
                int bw = Math.max(20, bf.stringWidth(text) + 12);
                int bh = 20;
                int bx = w - bw - 10;
                int by = (h - bh) / 2;
                g2.setColor(badgeColor);
                g2.fillRoundRect(bx, by, bw, bh, bh, bh);
                g2.setColor(Theme.BG);
                g2.drawString(text, bx + (bw - bf.stringWidth(text)) / 2, by + (bh - bf.getHeight()) / 2 + bf.getAscent());
            }
            g2.dispose();
        }
    }
}