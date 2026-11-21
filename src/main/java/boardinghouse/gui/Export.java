package boardinghouse.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// saves what a table shows (after searching and sorting) as a CSV file that Excel can open
public class Export {
    private Export() { }

    public static JButton csvButton(JTable table, String fileName) {
        JButton b = Theme.secondary("Export CSV");
        b.setToolTipText("Save this table as a file you can open in Excel");
        b.addActionListener(e -> saveCsv(table, fileName));
        return b;
    }

    public static void saveCsv(JTable table, String fileName) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(table, "There is nothing to export.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV file (opens in Excel)", "csv"));
        fc.setSelectedFile(new File(fileName + "_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
        if (file.exists() && !Theme.confirm(table, "Replace the existing file " + file.getName() + "?", "Export CSV")) return;

        List<Integer> columns = new ArrayList<>();
        for (int c = 0; c < table.getColumnCount(); c++) {
            if (!"Actions".equals(table.getColumnName(c))) columns.add(c);
        }

        StringBuilder sb = new StringBuilder("\uFEFF"); // lets Excel read the peso sign correctly
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(table.getColumnName(columns.get(i))));
        }
        sb.append("\r\n");
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(cell(table.getValueAt(r, columns.get(i))));
            }
            sb.append("\r\n");
        }

        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(table, "Saved " + table.getRowCount() + " row(s) to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(table, "Could not save the file. Close it if it is open in Excel.\n" + e.getMessage());
        }
    }

    private static String cell(Object value) {
        String s = value == null ? "" : value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}