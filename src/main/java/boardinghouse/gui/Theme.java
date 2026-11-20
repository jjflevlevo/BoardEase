package boardinghouse.gui;

import boardinghouse.model.Room;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Theme {
    // set to false if the dark title bar looks wrong on your computer
    private static final boolean DARK_TITLE_BAR = true;

    // ---------- Design tokens (from the UI/UX spec) ----------
    public static final Color BG = Color.decode("#0B1120");            // window background
    public static final Color SIDEBAR = Color.decode("#101827");       // sidebar and input background
    public static final Color CARD = Color.decode("#151F30");          // cards, panels, dialogs
    public static final Color CARD_HOVER = Color.decode("#1C293D");
    public static final Color BORDER = Color.decode("#263449");
    public static final Color BORDER_STRONG = Color.decode("#334155"); // secondary button border
    public static final Color ACCENT = Color.decode("#3B82F6");
    public static final Color ACCENT_HOVER = Color.decode("#2563EB");
    public static final Color TEXT = Color.decode("#F1F5F9");
    public static final Color MUTED = Color.decode("#94A3B8");         // secondary text
    public static final Color TEXT_DISABLED = Color.decode("#526176");
    public static final Color PLACEHOLDER = Color.decode("#64748B");
    public static final Color SUCCESS = Color.decode("#22C55E");
    public static final Color WARN = Color.decode("#F59E0B");
    public static final Color DANGER = Color.decode("#EF4444");
    public static final Color ROW_ALT = Color.decode("#121C2B");
    public static final Color ROW_SELECTED = Color.decode("#1D3555");
    public static final Color HEADER_BG = Color.decode("#1B2739");

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    // numbers sort by value, text sorts alphabetically (so 1500.00 comes after 200.00)
    private static final Comparator<Object> SMART = (a, b) -> {
        String x = a == null ? "" : a.toString().trim();
        String y = b == null ? "" : b.toString().trim();
        boolean nx = NUMBER.matcher(x).matches();
        boolean ny = NUMBER.matcher(y).matches();
        if (nx && ny) return Double.compare(Double.parseDouble(x), Double.parseDouble(y));
        if (nx) return -1;
        if (ny) return 1;
        return x.compareToIgnoreCase(y);
    };

    // item shown in room dropdowns (room == null means "All rooms")
    public record RoomItem(Room room) {
        @Override
        public String toString() {
            return room == null ? "All rooms" : "Room " + room.getRoomNo();
        }
    }

    // the spec gives sizes in pt, Swing uses pixels (96 dpi: px = pt x 4/3)
    public static Font font(float pt, int style) {
        return new Font("Segoe UI", style, Math.round(pt * 4f / 3f));
    }

    public static void install() {
        if (DARK_TITLE_BAR) {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
        UIManager.put("defaultFont", font(11, Font.PLAIN));
        FlatDarkLaf.setup();

        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("TitlePane.background", SIDEBAR);

        // inputs: 8 px radius, 1 px border, blue border when focused
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.error.borderColor", DANGER);
        UIManager.put("Component.error.focusedBorderColor", DANGER);
        UIManager.put("TextField.background", SIDEBAR);
        UIManager.put("PasswordField.background", SIDEBAR);
        UIManager.put("ComboBox.background", SIDEBAR);
        UIManager.put("TextField.placeholderForeground", PLACEHOLDER);
        UIManager.put("TextField.margin", new Insets(2, 12, 2, 12));
        UIManager.put("PasswordField.margin", new Insets(2, 12, 2, 12));

        // tables
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.alternateRowColor", ROW_ALT);
        UIManager.put("Table.selectionBackground", ROW_SELECTED);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.cellMargins", new Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.background", HEADER_BG);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.separatorColor", HEADER_BG);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);

        // tabs: blue underline on the active tab
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", MUTED);
        UIManager.put("TabbedPane.selectedForeground", TEXT);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.hoverColor", CARD_HOVER);
        UIManager.put("TabbedPane.tabHeight", 40);
    }

    // ---------- Containers ----------
    public static JPanel card() {
        return new RoundedPanel(CARD, BORDER, 12, 24);
    }

    // ---------- Buttons ----------
    private static JButton button(String text, String style) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(font(10, Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("FlatLaf.style", style);
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(d.width, 96), 40));
        return b;
    }

    // the one highlighted button on a screen (Von Restorff effect)
    public static JButton primary(String text) {
        return button(text, "background: #3B82F6; foreground: #FFFFFF; hoverBackground: #2563EB; "
                + "pressedBackground: #1D4ED8; focusWidth: 0; borderWidth: 0; arc: 8; margin: 0,16,0,16");
    }

    public static JButton secondary(String text) {
        return button(text, "background: #151F30; foreground: #F1F5F9; borderColor: #334155; "
                + "hoverBackground: #1C293D; pressedBackground: #1C293D; focusWidth: 0; arc: 8; margin: 0,16,0,16");
    }

    // destructive actions only (delete)
    public static JButton danger(String text) {
        return button(text, "background: #EF4444; foreground: #FFFFFF; hoverBackground: #DC2626; "
                + "pressedBackground: #B91C1C; focusWidth: 0; borderWidth: 0; arc: 8; margin: 0,16,0,16");
    }

    // text that looks like a link
    public static JButton link(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(ACCENT);
        b.setFont(font(10, Font.BOLD));
        b.setMargin(new Insets(0, 4, 0, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // changes the text and recalculates the width so long text is not cut off
    public static void setButtonText(JButton b, String text) {
        b.setText(text);
        b.setPreferredSize(null);
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(d.width, 96), 40));
    }

    // ---------- Labels ----------
    // page title: 22 pt bold
    public static JLabel pageTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(22, Font.BOLD));
        return l;
    }

    // section title: 15 pt bold
    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(15, Font.BOLD));
        return l;
    }

    // small secondary text: 9 pt
    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(font(9, Font.PLAIN));
        return l;
    }

    // inline error message under a field (9 pt, red)
    public static JLabel error() {
        JLabel l = new JLabel(" ");
        l.setForeground(DANGER);
        l.setFont(font(9, Font.PLAIN));
        return l;
    }

    // ---------- Inputs ----------
    public static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        addContextMenu(f);
        return f;
    }

    // password field with a Show / Hide button (click to toggle)
    public static JPasswordField password(String placeholder) {
        JPasswordField f = new JPasswordField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        char hidden = f.getEchoChar();

        JToggleButton toggle = new JToggleButton("Show");
        toggle.putClientProperty("JButton.buttonType", "toolBarButton");
        toggle.setFocusable(false);
        toggle.setForeground(MUTED);
        toggle.setFont(font(9, Font.BOLD));
        toggle.setToolTipText("Show or hide the password");
        toggle.addActionListener(e -> {
            boolean show = toggle.isSelected();
            f.setEchoChar(show ? (char) 0 : hidden);
            toggle.setText(show ? "Hide" : "Show");
        });
        f.putClientProperty("JTextField.trailingComponent", toggle);

        addContextMenu(f);
        return f;
    }

    // read-only text that can still be selected and copied
    public static JTextField selectable(String text) {
        JTextField f = new JTextField(text == null ? "" : text);
        f.setEditable(false);
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder());
        addContextMenu(f);
        return f;
    }

    // right-click menu: Cut, Copy, Paste, Select All
    // (Java blocks Cut and Copy on password fields on purpose, so those two are left out)
    public static void addContextMenu(JTextComponent c) {
        boolean password = c instanceof JPasswordField;
        boolean editable = c.isEditable();

        JMenuItem cut = new JMenuItem(new DefaultEditorKit.CutAction());
        cut.setText("Cut");
        JMenuItem copy = new JMenuItem(new DefaultEditorKit.CopyAction());
        copy.setText("Copy");
        JMenuItem paste = new JMenuItem(new DefaultEditorKit.PasteAction());
        paste.setText("Paste");
        JMenuItem all = new JMenuItem("Select All");
        all.addActionListener(e -> {
            c.requestFocusInWindow();
            c.selectAll();
        });

        JPopupMenu menu = new JPopupMenu();
        if (!password && editable) menu.add(cut);
        if (!password) menu.add(copy);
        if (editable) menu.add(paste);
        menu.addSeparator();
        menu.add(all);

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                c.requestFocusInWindow();
                boolean selected = c.getSelectedText() != null;
                cut.setEnabled(selected);
                copy.setEnabled(selected);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
        c.setComponentPopupMenu(menu);
    }

    // red border on a field (or back to normal)
    public static void markError(JComponent field, boolean on) {
        field.putClientProperty("JComponent.outline", on ? "error" : null);
    }

    // shows or clears the error for one field and returns whether it was valid
    public static boolean check(JComponent field, JLabel error, boolean valid, String message) {
        markError(field, !valid);
        error.setText(valid ? " " : message);
        return valid;
    }

    // clears a field's error as soon as the user edits it
    public static void clearErrorOnEdit(JTextComponent field, JLabel error) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void clear() {
                markError(field, false);
                error.setText(" ");
            }

            @Override public void insertUpdate(DocumentEvent e) { clear(); }
            @Override public void removeUpdate(DocumentEvent e) { clear(); }
            @Override public void changedUpdate(DocumentEvent e) { clear(); }
        });
    }

    // ---------- Layout helpers ----------
    // left aligned, full width, fixed height
    public static <T extends JComponent> T block(T c, int height) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return c;
    }

    public static Component gap(int height) {
        return Box.createVerticalStrut(height);
    }

    // label (20) + 4 + input (40) + error message (20) = 84 px tall
    public static JPanel fieldRow(String label, JComponent field, JLabel error) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(font(10, Font.PLAIN));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(block(l, 20));
        p.add(Box.createVerticalStrut(4));
        p.add(block(field, 40));
        p.add(block(error, 20));
        return block(p, 84);
    }

    // label sits right above its field (Proximity law)
    public static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = muted(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        block(field, 40);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        block(p, 66);
        return p;
    }

    public static JPanel form(Component... items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < items.length; i++) {
            if (i > 0) p.add(Box.createVerticalStrut(10));
            if (items[i] instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(items[i]);
        }
        p.setPreferredSize(new Dimension(340, p.getPreferredSize().height));
        return p;
    }

    // label and value pairs, for profile windows (values can be selected and copied)
    public static JPanel detailGrid(String[][] rows) {
        JPanel g = new JPanel(new GridBagLayout());
        g.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 28);
        for (int i = 0; i < rows.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            g.add(muted(rows[i][0]), c);
            c.gridx = 1;
            g.add(selectable(rows[i][1]), c);
        }
        return g;
    }

    public static JPanel header(String text, JComponent right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(title(text), BorderLayout.WEST);
        if (right != null) p.add(right, BorderLayout.EAST);
        return p;
    }

    public static JPanel buttonRow(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    // ---------- Tables ----------
    // Tables allow several selected rows (Ctrl or Shift + click)
    public static JTable table(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable t = new JTable(model);
        t.setFont(font(10, Font.PLAIN));
        t.setRowHeight(44);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDER);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setFont(font(9, Font.BOLD));
        h.setPreferredSize(new Dimension(0, 40));
        return t;
    }

    public static void singleSelect(JTable t) {
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    // selected rows as positions in the data (still correct after sorting or searching)
    public static int[] selectedModelRows(JTable t) {
        int[] view = t.getSelectedRows();
        int[] model = new int[view.length];
        for (int i = 0; i < view.length; i++) model[i] = t.convertRowIndexToModel(view[i]);
        return model;
    }

    // search box plus click-to-sort headers for a table
    public static JPanel searchBar(JTable table) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        for (int i = 0; i < table.getColumnCount(); i++) sorter.setComparator(i, SMART);
        table.setRowSorter(sorter);

        JTextField search = field("Search...");
        search.putClientProperty("JTextField.showClearButton", true);
        search.setPreferredSize(new Dimension(280, 40));

        Runnable apply = () -> {
            String text = search.getText().trim();
            if (text.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apply.run(); }
            @Override public void removeUpdate(DocumentEvent e) { apply.run(); }
            @Override public void changedUpdate(DocumentEvent e) { apply.run(); }
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(search);

        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.add(left, BorderLayout.WEST);
        p.add(muted("Click a column header to sort. Ctrl or Shift + click selects several rows."), BorderLayout.EAST);
        return p;
    }

    public static JPanel northWithSearch(JComponent header, JTable table) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.add(header, BorderLayout.NORTH);
        p.add(searchBar(table), BorderLayout.CENTER);
        return p;
    }

    public static void fill(JTable t, List<Object[]> rows) {
        DefaultTableModel model = (DefaultTableModel) t.getModel();
        model.setRowCount(0);
        for (Object[] r : rows) model.addRow(r);
    }

    // status text colors: PAID green, PARTIAL and PENDING amber, OVERDUE and REJECTED red
    public static void statusColumn(JTable t, int column) {
        t.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tb, value, selected, focus, row, col);
                String s = value == null ? "" : value.toString();
                c.setFont(tb.getFont().deriveFont(Font.BOLD));
                if (s.contains("OVERDUE") || s.equals("REJECTED")) c.setForeground(DANGER);
                else if (s.equals("PAID") || s.equals("APPROVED")) c.setForeground(SUCCESS);
                else if (s.equals("PARTIAL") || s.equals("PENDING")) c.setForeground(WARN);
                else c.setForeground(tb.getForeground());
                return c;
            }
        });
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setBorder(BorderFactory.createLineBorder(BORDER));
        s.getViewport().setBackground(CARD);
        return s;
    }

    // ---------- Misc ----------
    public static String money(double v) {
        return String.format("%.2f", v);
    }

    // makes text safe to show inside an HTML label
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static boolean confirmWarn(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static DefaultComboBoxModel<RoomItem> roomModel(List<Room> rooms, boolean includeAll) {
        DefaultComboBoxModel<RoomItem> m = new DefaultComboBoxModel<>();
        if (includeAll) m.addElement(new RoomItem(null));
        for (Room r : rooms) m.addElement(new RoomItem(r));
        return m;
    }
}