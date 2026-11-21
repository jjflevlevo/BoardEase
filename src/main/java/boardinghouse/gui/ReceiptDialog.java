package boardinghouse.gui;

import boardinghouse.dao.BillDAO;
import boardinghouse.dao.RoomDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Bill;
import boardinghouse.model.Payment;
import boardinghouse.model.Room;
import boardinghouse.model.Tenant;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// a printable receipt for an approved payment (print, save as PDF, or save as an image)
public class ReceiptDialog extends JDialog {
    private final Paper paper;
    private final String receiptNo;

    public static void show(Component parent, Payment p) {
        if (p == null) return;
        if (!"APPROVED".equals(p.getStatus())) {
            JOptionPane.showMessageDialog(parent, "Receipts are only available for approved payments.");
            return;
        }
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        new ReceiptDialog(owner, p).setVisible(true);
    }

    private ReceiptDialog(Window owner, Payment p) {
        super(owner, "Payment Receipt", ModalityType.APPLICATION_MODAL);
        receiptNo = String.format("BE-%06d", p.getPaymentId());

        String[][] rows = {
                {"Receipt No.", receiptNo},
                {"Payment Date", Ui.localDate(p.getPaidAt())},
                {"Tenant", p.getTenantName()},
                {"Room", roomOf(p)},
                {"Bill", typeName(p.getBillType())},
                {"Billing Month", p.getBillingMonth()},
                {"Payment Method", methodName(p.getMethod())},
                {"Reference No.", p.getRefNo() == null || p.getRefNo().isBlank() ? "-" : p.getRefNo()}};
        paper = new Paper(rows, Ui.peso(p.getAmount()));

        JButton close = Theme.secondary("Close");
        close.addActionListener(e -> dispose());
        JButton save = Theme.secondary("Save as Image");
        save.addActionListener(e -> saveImage());
        JButton print = Theme.primary("Print / Save as PDF");
        print.addActionListener(e -> print());
        JLabel tip = Theme.muted("To save a PDF, click Print and choose \"Microsoft Print to PDF\" as the printer.");

        JPanel paperWrap = new JPanel(new GridBagLayout());
        paperWrap.setOpaque(false);
        paperWrap.add(paper);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(tip, BorderLayout.NORTH);
        south.add(Theme.buttonRow(close, save, print), BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));
        root.add(paperWrap, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static String typeName(String type) {
        if ("RENT".equals(type)) return "Rent";
        if ("WATER".equals(type)) return "Water";
        if ("ELECTRIC".equals(type)) return "Electricity";
        return type == null ? "-" : type;
    }

    private static String methodName(String method) {
        if ("GCASH".equals(method)) return "GCash";
        if ("DEPOSIT".equals(method)) return "Security deposit";
        return "Cash";
    }

    private static String roomOf(Payment p) {
        Bill bill = new BillDAO().findById(p.getBillId());
        if (bill == null) return "-";
        Tenant t = new TenantDAO().findById(bill.getTenantId());
        if (t == null || t.getRoomId() == null) return "-";
        Room r = new RoomDAO().findById(t.getRoomId());
        return r == null ? "-" : r.getRoomNo();
    }

    // ---------- Print and save ----------
    private void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("BoardEase receipt " + receiptNo);
        job.setPrintable((graphics, format, page) -> {
            if (page > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            double scale = Math.min(1.0, Math.min(format.getImageableWidth() / Paper.W,
                    format.getImageableHeight() / Paper.H));
            g2.translate(format.getImageableX() + (format.getImageableWidth() - Paper.W * scale) / 2,
                    format.getImageableY());
            g2.scale(scale, scale);
            paper.paintReceipt(g2, false);
            return Printable.PAGE_EXISTS;
        });
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Could not print: " + e.getMessage());
        }
    }

    private void saveImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        fc.setSelectedFile(new File("receipt_" + receiptNo + ".png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) file = new File(file.getParentFile(), file.getName() + ".png");

        BufferedImage img = new BufferedImage(Paper.W * 2, Paper.H * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(2, 2);
        paper.paintReceipt(g, false);
        g.dispose();
        try {
            ImageIO.write(img, "png", file);
            JOptionPane.showMessageDialog(this, "Saved:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save the image: " + e.getMessage());
        }
    }

    // ---------- The receipt itself (white paper, drawn with Java2D) ----------
    private static class Paper extends JComponent {
        static final int W = 420;
        static final int H = 560;
        private static final Color INK = new Color(0x0F172A);
        private static final Color GRAY = new Color(0x64748B);
        private static final Color LINE = new Color(0xCBD5E1);

        private final String[][] rows;
        private final String amount;

        Paper(String[][] rows, String amount) {
            this.rows = rows;
            this.amount = amount;
            Dimension d = new Dimension(W, H);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintReceipt(g2, true);
            g2.dispose();
        }

        void paintReceipt(Graphics2D g2, boolean rounded) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            if (rounded) g2.fill(new RoundRectangle2D.Float(0, 0, W, H, 16, 16));
            else g2.fillRect(0, 0, W, H);

            // BoardEase wordmark
            g2.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 30));
            FontMetrics bm = g2.getFontMetrics();
            int bx = (W - bm.stringWidth("BoardEase")) / 2;
            g2.setColor(INK);
            g2.drawString("Board", bx, 56);
            g2.setColor(Theme.ACCENT);
            g2.drawString("Ease", bx + bm.stringWidth("Board"), 56);

            center(g2, "Rent and Utilities Billing System", new Font("Segoe UI", Font.PLAIN, 12), GRAY, 80);
            center(g2, "PAYMENT RECEIPT", new Font("Segoe UI", Font.BOLD, 15), INK, 116);
            dashed(g2, 136);

            Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
            Font valueFont = new Font("Segoe UI", Font.BOLD, 13);
            int y = 166;
            for (String[] r : rows) {
                g2.setFont(labelFont);
                g2.setColor(GRAY);
                g2.drawString(r[0], 32, y);
                g2.setFont(valueFont);
                g2.setColor(INK);
                FontMetrics fm = g2.getFontMetrics();
                String value = fit(fm, r[1], W - 32 - 150);
                g2.drawString(value, W - 32 - fm.stringWidth(value), y);
                y += 30;
            }

            y -= 10;
            dashed(g2, y);
            y += 38;
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(INK);
            g2.drawString("AMOUNT PAID", 32, y);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g2.setColor(Theme.ACCENT);
            FontMetrics am = g2.getFontMetrics();
            g2.drawString(amount, W - 32 - am.stringWidth(amount), y + 2);

            // status pill
            y += 26;
            String status = "APPROVED";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics pm = g2.getFontMetrics();
            int pw = pm.stringWidth(status) + 24;
            int ph = 24;
            int px = W - 32 - pw;
            g2.setColor(new Color(0xDCFCE7));
            g2.fillRoundRect(px, y, pw, ph, ph, ph);
            g2.setColor(new Color(0x15803D));
            g2.drawString(status, px + 12, y + (ph - pm.getHeight()) / 2 + pm.getAscent());

            center(g2, "Thank you for your payment!", new Font("Segoe UI", Font.BOLD, 12), INK, H - 46);
            center(g2, "Generated by BoardEase on " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    new Font("Segoe UI", Font.PLAIN, 11), GRAY, H - 26);
        }

        private static void center(Graphics2D g2, String s, Font f, Color c, int y) {
            g2.setFont(f);
            g2.setColor(c);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (W - fm.stringWidth(s)) / 2, y);
        }

        private static void dashed(Graphics2D g2, int y) {
            Stroke old = g2.getStroke();
            g2.setColor(LINE);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));
            g2.drawLine(24, y, W - 24, y);
            g2.setStroke(old);
        }

        private static String fit(FontMetrics fm, String s, int max) {
            if (s == null) return "-";
            if (fm.stringWidth(s) <= max) return s;
            while (s.length() > 1 && fm.stringWidth(s + "...") > max) s = s.substring(0, s.length() - 1);
            return s + "...";
        }
    }
}