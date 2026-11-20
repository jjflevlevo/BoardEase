package boardinghouse.service;

import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.BillDAO;
import boardinghouse.dao.PaymentDAO;
import boardinghouse.dao.SettingsDAO;
import boardinghouse.model.Bill;
import boardinghouse.model.Payment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public class PaymentService {
    private static final long MAX_PROOF_BYTES = 5L * 1024 * 1024;

    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SettingsDAO settings = new SettingsDAO();
    private final ActivityDAO activity = new ActivityDAO();

    private static String peso(double v) {
        return String.format("\u20B1%,.2f", v);
    }

    // Remaining amount to pay: bill + late fee - approved payments
    public double balanceOf(Bill bill) {
        if ("PAID".equals(bill.getStatus())) return 0;
        double balance = bill.getAmount() + bill.getLateFee(LocalDate.now()) - paymentDAO.sumApproved(bill.getBillId());
        return Math.max(0, Math.round(balance * 100.0) / 100.0);
    }

    // Balance minus payments still waiting for approval
    private double availableToPay(Bill bill) {
        return balanceOf(bill) - paymentDAO.sumPending(bill.getBillId());
    }

    // Overload 1: simple cash payment
    public String submitPayment(int tenantId, int billId, double amount) {
        return submitPayment(tenantId, billId, amount, "CASH", null, null);
    }

    // Overload 2: payment with a method
    public String submitPayment(int tenantId, int billId, double amount, String method) {
        return submitPayment(tenantId, billId, amount, method, null, null);
    }

    // Overload 3: payment with a method and reference no.
    public String submitPayment(int tenantId, int billId, double amount, String method, String refNo) {
        return submitPayment(tenantId, billId, amount, method, refNo, null);
    }

    // Overload 4: payment with a method, reference no., and a proof image (GCash)
    public String submitPayment(int tenantId, int billId, double amount, String method, String refNo, File proof) {
        Bill bill = billDAO.findById(billId);
        if (bill == null || bill.getTenantId() != tenantId) return "Bill not found.";
        if ("PAID".equals(bill.getStatus())) return "This bill is already fully paid.";
        if (amount <= 0) return "Amount must be greater than 0.";

        boolean online = "GCASH".equals(method);
        if (online) {
            if (!settings.hasGcash()) {
                return "Pay Online is unavailable. The owner has not given any GCash information yet.";
            }
            if (refNo == null || refNo.isBlank()) return "GCash payments need a reference number.";
            if (proof != null) {
                String lower = proof.getName().toLowerCase();
                if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                    return "The proof must be a PNG or JPG image.";
                }
                if (!proof.isFile()) return "The proof file was not found.";
                if (proof.length() > MAX_PROOF_BYTES) return "The proof image is too large (max 5 MB).";
            }
        } else {
            proof = null;
        }

        double available = availableToPay(bill);
        if (available <= 0) return "Your pending payments already cover this bill. Wait for the owner's approval.";
        if (amount > available + 0.005) return String.format("Amount is too high. You can pay up to %.2f.", available);

        String proofPath = null;
        if (proof != null) {
            try {
                proofPath = saveProof(proof, tenantId);
            } catch (IOException e) {
                return "Could not save the proof image. Try again.";
            }
        }

        Payment p = new Payment(billId, amount, method, refNo);
        p.setProofPath(proofPath);
        paymentDAO.insert(p);
        activity.log("PENDING", "Payment of " + peso(amount) + " submitted by " + bill.getTenantName()
                + " (" + bill.getType() + ", " + bill.getBillingMonth() + ")");
        return "Payment submitted. Waiting for the owner's approval.";
    }

    private String saveProof(File source, int tenantId) throws IOException {
        String name = source.getName().toLowerCase();
        String ext = name.substring(name.lastIndexOf('.') + 1);
        Path dir = Path.of("uploads");
        Files.createDirectories(dir);
        Path dest = dir.resolve("proof_" + tenantId + "_" + System.currentTimeMillis() + "." + ext);
        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString().replace('\\', '/');
    }

    public String approve(int paymentId) {
        Payment p = paymentDAO.findById(paymentId);
        if (p == null) return "Payment not found.";
        if (!"PENDING".equals(p.getStatus())) return "This payment was already " + p.getStatus().toLowerCase() + ".";

        paymentDAO.updateStatus(paymentId, "APPROVED");

        Bill bill = billDAO.findById(p.getBillId());
        double total = bill.getAmount() + bill.getLateFee(LocalDate.now());
        double paid = paymentDAO.sumApproved(bill.getBillId());
        String status = paid + 0.005 >= total ? "PAID" : (paid > 0 ? "PARTIAL" : "UNPAID");
        billDAO.updateStatus(bill.getBillId(), status);
        activity.log("PAID", "Payment of " + peso(p.getAmount()) + " approved for " + p.getTenantName());
        return "Payment approved. Bill status: " + status + ".";
    }

    public String reject(int paymentId) {
        Payment p = paymentDAO.findById(paymentId);
        if (p == null) return "Payment not found.";
        if (!"PENDING".equals(p.getStatus())) return "This payment was already " + p.getStatus().toLowerCase() + ".";
        paymentDAO.updateStatus(paymentId, "REJECTED");
        activity.log("REJECTED", "Payment of " + peso(p.getAmount()) + " rejected for " + p.getTenantName());
        return "Payment rejected.";
    }
}