package boardinghouse.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

// clickable buttons inside a table column (edit, delete, approve, and so on)
public class RowActions {
    private static final int GAP = 8;
    private static final int PAD = 12;
    private static final int HEIGHT = 28;
    private static final DefaultTableCellRenderer BASE = new DefaultTableCellRenderer();

    private RowActions() { }

    // icon or text (one of them is null), color, tooltip, when to show it, and what to do (gets the data row)
    public record RowAction(String icon, String text, Color color, String tooltip,
                            IntPredicate visible, IntConsumer onClick) { }

    private record Slot(RowAction action, Rectangle bounds) { }

    private record Hit(RowAction action, int modelRow) { }

    public static RowAction icon(String icon, Color color, String tooltip, IntConsumer onClick) {
        return new RowAction(icon, null, color, tooltip, row -> true, onClick);
    }

    public static RowAction text(String text, Color color, String tooltip, IntConsumer onClick) {
        return new RowAction(null, text, color, tooltip, row -> true, onClick);
    }

    // shows the button only for the rows where the condition is true
    public static RowAction when(RowAction a, IntPredicate visible) {
        return new RowAction(a.icon(), a.text(), a.color(), a.tooltip(), visible, a.onClick());
    }

    // call this after Ui.toolbar(table, ...) so the column can be made unsortable
    public static void install(JTable table, int column, RowAction... actions) {
        FontMetrics fm = table.getFontMetrics(Theme.font(9, Font.BOLD));
        int width = PAD * 2;
        for (int i = 0; i < actions.length; i++) {
            width += buttonWidth(fm, actions[i]);
            if (i > 0) width += GAP;
        }
        TableColumn col = table.getColumnModel().getColumn(column);
        col.setMinWidth(width);
        col.setMaxWidth(width);
        col.setPreferredWidth(width);

        if (table.getRowSorter() instanceof TableRowSorter<?> sorter) sorter.setSortable(column, false);

        col.setCellRenderer((t, value, selected, focus, row, c) -> {
            Color bg = BASE.getTableCellRendererComponent(t, "", selected, false, row, c).getBackground();
            int modelRow = t.convertRowIndexToModel(row);
            return new Cell(slots(t, actions, modelRow, t.getRowHeight(row)), bg);
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Hit hit = hitAt(table, column, actions, e);
                if (hit != null) hit.action().onClick().accept(hit.modelRow());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Hit hit = hitAt(table, column, actions, e);
                table.setCursor(hit == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                table.setToolTipText(hit == null ? null : hit.action().tooltip());
            }
        };
        table.addMouseListener(mouse);
        table.addMouseMotionListener(mouse);
    }

    private static int buttonWidth(FontMetrics fm, RowAction a) {
        return a.text() == null ? 32 : fm.stringWidth(a.text()) + 28;
    }

    private static List<Slot> slots(JTable table, RowAction[] actions, int modelRow, int cellHeight) {
        FontMetrics fm = table.getFontMetrics(Theme.font(9, Font.BOLD));
        List<Slot> list = new ArrayList<>();
        int x = PAD;
        int y = (cellHeight - HEIGHT) / 2;
        for (RowAction a : actions) {
            if (!a.visible().test(modelRow)) continue;
            int w = buttonWidth(fm, a);
            list.add(new Slot(a, new Rectangle(x, y, w, HEIGHT)));
            x += w + GAP;
        }
        return list;
    }

    private static Hit hitAt(JTable table, int column, RowAction[] actions, MouseEvent e) {
        int viewRow = table.rowAtPoint(e.getPoint());
        int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow < 0 || viewCol != column) return null;

        Rectangle cell = table.getCellRect(viewRow, viewCol, false);
        int modelRow = table.convertRowIndexToModel(viewRow);
        int px = e.getX() - cell.x;
        int py = e.getY() - cell.y;
        for (Slot s : slots(table, actions, modelRow, cell.height)) {
            if (s.bounds().contains(px, py)) return new Hit(s.action(), modelRow);
        }
        return null;
    }

    private static class Cell extends JComponent {
        private final List<Slot> slots;
        private final Color bg;

        Cell(List<Slot> slots, Color bg) {
            this.slots = slots;
            this.bg = bg;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(Theme.font(9, Font.BOLD));
            FontMetrics fm = g2.getFontMetrics();
            for (Slot s : slots) {
                Rectangle b = s.bounds();
                g2.setColor(s.action().color());
                g2.fillRoundRect(b.x, b.y, b.width, b.height, 8, 8);
                g2.setColor(Color.WHITE);
                if (s.action().icon() != null) {
                    Icons.draw(g2, s.action().icon(), b.x + (b.width - 16) / 2, b.y + (b.height - 16) / 2, 16, Color.WHITE);
                } else {
                    String text = s.action().text();
                    g2.drawString(text, b.x + (b.width - fm.stringWidth(text)) / 2,
                            b.y + (b.height - fm.getHeight()) / 2 + fm.getAscent());
                }
            }
            g2.dispose();
        }
    }
}