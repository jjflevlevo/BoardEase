package boardinghouse.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// shared pieces of the design: cards, tabs, text areas, the table toolbar and footer, status pills, the bell
public class Ui {
    private Ui() { }

    private static final DefaultTableCellRenderer BASE = new DefaultTableCellRenderer();

    public static String peso(double v) {
        return String.format("\u20B1%,.2f", v);
    }

    // the database saves times in UTC, so this converts to this computer's time zone (yyyy-MM-dd)
    public static String localDate(String utc) {
        if (utc == null) return "-";
        try {
            LocalDateTime t = LocalDateTime.parse(utc.trim().replace(' ', 'T'));
            return t.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return utc;
        }
    }

    // one line of text, cut to a maximum length
    public static String preview(String s, int max) {
        if (s == null) return "-";
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > max ? one.substring(0, max - 3) + "..." : one;
    }

    // ---------- Cards ----------
    public static JPanel card(JComponent body) {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.BORDER, 12, 20);
        card.setLayout(new BorderLayout());
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // a card with a header row on top and content below
    public static JPanel panelCard(JComponent header, JComponent body) {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.BORDER, 12, 20);
        card.setLayout(new BorderLayout(0, 14));
        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    public static JPanel topBar(JLabel title, JComponent right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // tabs with a blue line under the active tab
    public static JTabbedPane tabs() {
        JTabbedPane t = new JTabbedPane();
        t.putClientProperty("JTabbedPane.tabType", "underlined");
        t.putClientProperty("JTabbedPane.tabHeight", 40);
        t.putClientProperty("JTabbedPane.tabInsets", new Insets(0, 16, 0, 16));
        t.setOpaque(false);
        return t;
    }

    // ---------- Multi-line text ----------
    public static JTextArea textArea(String placeholder, int rows) {
        JTextArea a = new JTextArea(rows, 30);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.font(11, Font.PLAIN));
        a.setBackground(Theme.SIDEBAR);
        a.setMargin(new Insets(8, 10, 8, 10));
        a.putClientProperty("JTextField.placeholderText", placeholder);
        Theme.addContextMenu(a);
        return a;
    }

    public static JScrollPane areaScroll(JTextArea area) {
        JScrollPane s = new JScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return s;
    }

    // a label with any component under it, at a fixed height
    public static JPanel labeledBlock(String label, JComponent content, int height) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = Theme.muted(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        Theme.block(content, height);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(content);
        return Theme.block(p, height + 24);
    }

    // ---------- Table toolbar and footer ----------
    // search box on the left, an optional component (like an Add button) on the right
    // (this also turns on click-to-sort headers, so call it before RowActions.install)
    public static JPanel toolbar(JTable table, JComponent right) {
        JPanel search = Theme.searchBar(table);
        if (search.getLayout() instanceof BorderLayout bl) {
            Component east = bl.getLayoutComponent(BorderLayout.EAST);
            if (east != null) search.remove(east);
        }
        if (right != null) search.add(right, BorderLayout.EAST);
        return search;
    }

    // "Showing 3 of 8" that follows the search box (the sorting tip is on the column headers)
    public static JLabel countLabel(JTable table) {
        JLabel label = Theme.muted(" ");
        Runnable update = () -> label.setText("Showing " + table.getRowCount() + " of " + table.getModel().getRowCount());
        table.getModel().addTableModelListener(e -> SwingUtilities.invokeLater(update));
        if (table.getRowSorter() != null) table.getRowSorter().addRowSorterListener(e -> update.run());
        table.getTableHeader().setToolTipText("Click a column header to sort. Ctrl or Shift + click selects several rows.");
        update.run();
        return label;
    }

    public static JPanel footer(JTable table, JComponent... buttons) {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(countLabel(table));

        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.add(left, BorderLayout.WEST);
        if (buttons.length > 0) p.add(Theme.buttonRow(buttons), BorderLayout.EAST);
        return p;
    }

    // the color of the line between table rows (null when the table has no row lines)
    public static Color rowLine(JTable table) {
        return table.getShowHorizontalLines() ? table.getGridColor() : null;
    }

    // ---------- Status pills in tables ----------
    public static Color statusColor(String text) {
        String s = text == null ? "" : text.toUpperCase();
        if (s.contains("OVERDUE") || s.equals("REJECTED") || s.equals("INACTIVE") || s.equals("IMPORTANT")) {
            return Theme.DANGER;
        }
        if (s.equals("PAID") || s.equals("APPROVED") || s.equals("OCCUPIED") || s.equals("ACTIVE")
                || s.equals("DONE") || s.equals("RETURNED")) {
            return Theme.SUCCESS;
        }
        if (s.equals("PARTIAL") || s.equals("PENDING") || s.equals("VACANT") || s.equals("UNPAID")
                || s.equals("UNASSIGNED") || s.equals("OPEN")) {
            return Theme.WARN;
        }
        if (s.equals("IN PROGRESS") || s.equals("HELD")) return Theme.ACCENT;
        return Theme.MUTED;
    }

    // shows the text of a column as a colored rounded label
    public static void statusPills(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Color bg = BASE.getTableCellRendererComponent(t, "", selected, false, row, col).getBackground();
                return new PillCell(value == null ? "" : value.toString(), bg, rowLine(t));
            }
        });
    }

    private static class PillCell extends JComponent {
        private final String text;
        private final Color bg;
        private final Color line;

        PillCell(String text, Color bg, Color line) {
            this.text = text;
            this.bg = bg;
            this.line = line;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (line != null) {
                g2.setColor(line);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1); // keeps the line between rows
            }
            if (!text.isEmpty()) {
                Color c = statusColor(text);
                g2.setFont(Theme.font(8.5f, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                int pw = fm.stringWidth(text) + 20;
                int ph = 22;
                int x = 12;
                int y = (getHeight() - ph) / 2;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 38));
                g2.fillRoundRect(x, y, pw, ph, ph, ph);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 130));
                g2.drawRoundRect(x, y, pw - 1, ph - 1, ph, ph);
                g2.setColor(c);
                g2.drawString(text, x + 10, y + (ph - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // ---------- Notification bell ----------
    public static class Bell extends JComponent {
        private int count;
        private boolean hover;

        public Bell(Runnable onClick) {
            setPreferredSize(new Dimension(40, 40));
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
                    onClick.run();
                }
            });
        }

        public void setCount(int count) {
            this.count = count;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(Theme.CARD_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            Icons.draw(g2, "bell", 10, 10, 20, hover ? Theme.TEXT : Theme.MUTED);
            if (count > 0) {
                String text = count > 9 ? "9+" : String.valueOf(count);
                g2.setFont(Theme.font(8, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                int d = 16;
                int x = getWidth() - d - 2;
                int y = 2;
                g2.setColor(Theme.DANGER);
                g2.fillOval(x, y, d, d);
                g2.setColor(Color.WHITE);
                g2.drawString(text, x + (d - fm.stringWidth(text)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}