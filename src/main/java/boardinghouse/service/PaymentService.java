package boardinghouse.service;

import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.BillDAO;
import boardinghouse.dao.PaymentDAO;
import boardinghouse.dao.SettingsDAO;
import boardinghouse.exception.InvalidPaymentException;
import boardinghouse.model.Bill;
import boardinghouse.model.BillStatus;
import boardinghouse.model.Payment;
import boardinghouse.model.PaymentMethod;
import boardinghouse.model.PaymentStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public class PaymentService {
    public static final String SUBMITTED = "Payment submitted. Waiting for the owner's approval.";
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
        if (bill.isPaid()) return 0;
        double balance = bill.getAmount() + bill.getLateFee(LocalDate.now()) - paymentDAO.sumApproved(bill.getBillId());
        return Math.max(0, Math.round(balance * 100.0) / 100.0);
    }

    // Balance minus payments still waiting for approval
    private double availableToPay(Bill bill) {
        return balanceOf(bill) - paymentDAO.sumPending(bill.getBillId());
    }

    // ---------- Paying ----------
    // Checks and saves a payment. Throws InvalidPaymentException with a message the user can read.
    public void pay(int tenantId, int billId, double amount, PaymentMethod method, String refNo, File proof)
            throws InvalidPaymentException {
        if (method == null) throw new InvalidPaymentException("Choose a payment method.");
        if (method == PaymentMethod.DEPOSIT) {
            throw new InvalidPaymentException("Deposits are applied by the owner when a tenant moves out.");
        }

        Bill bill = billDAO.findById(billId);
        if (bill == null || bill.getTenantId() != tenantId) throw new InvalidPaymentException("Bill not found.");
        if (bill.isPaid()) throw new InvalidPaymentException("This bill is already fully paid.");
        if (amount <= 0) throw new InvalidPaymentException("Amount must be greater than 0.");

        if (method.isOnline()) {
            if (!settings.hasGcash()) {
                throw new InvalidPaymentException("Pay Online is unavailable. The owner has not given any GCash information yet.");
            }
            if (refNo == null || refNo.isBlank()) throw new InvalidPaymentException("GCash payments need a reference number.");
            if (proof != null) checkProof(proof);
        } else {
            proof = null;
        }

        double available = availableToPay(bill);
        if (available <= 0) {
            throw new InvalidPaymentException("Your pending payments already cover this bill. Wait for the owner's approval.");
        }
        if (amount > available + 0.005) {
            throw new InvalidPaymentException(String.format("Amount is too high. You can pay up to %.2f.", available));
        }

        String proofPath = null;
        if (proof != null) {
            try {
                proofPath = saveProof(proof, tenantId);
            } catch (IOException e) {
                throw new InvalidPaymentException("Could not save the proof image. Try again.");
            }
        }

        Payment p = new Payment(billId, amount, method, refNo);
        p.setProofPath(proofPath);
        paymentDAO.insert(p);
        activity.log("PENDING", "Payment of " + peso(amount) + " submitted by " + bill.getTenantName()
                + " (" + bill.getType() + ", " + bill.getBillingMonth() + ")");
    }

    private void checkProof(File proof) throws InvalidPaymentException {
        String lower = proof.getName().toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            throw new InvalidPaymentException("The proof must be a PNG or JPG image.");
        }
        if (!proof.isFile()) throw new InvalidPaymentException("The proof file was not found.");
        if (proof.length() > MAX_PROOF_BYTES) throw new InvalidPaymentException("The proof image is too large (max 5 MB).");
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

    // ---------- Overloads used by the screens: they return a message instead of throwing ----------
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
        try {
            pay(tenantId, billId, amount, PaymentMethod.from(method), refNo, proof);
            return SUBMITTED;
        } catch (InvalidPaymentException e) {
            return e.getMessage();
        } catch (IllegalArgumentException e) {
            return "Unknown payment method.";
        }
    }

    // ---------- Owner review ----------
    public String approve(int paymentId) {
        Payment p = paymentDAO.findById(paymentId);
        if (p == null) return "Payment not found.";
        if (p.getPaymentStatus().isFinal()) {
            return "This payment was already " + p.getPaymentStatus().label().toLowerCase() + ".";
        }

        paymentDAO.updateStatus(paymentId, PaymentStatus.APPROVED.name());

        Bill bill = billDAO.findById(p.getBillId());
        double total = bill.getAmount() + bill.getLateFee(LocalDate.now());
        BillStatus status = BillStatus.fromAmounts(paymentDAO.sumApproved(bill.getBillId()), total);
        billDAO.updateStatus(bill.getBillId(), status.name());
        activity.log("PAID", "Payment of " + peso(p.getAmount()) + " approved for " + p.getTenantName());
        return "Payment approved. Bill status: " + status.name() + ".";
    }

    public String reject(int paymentId) {
        Payment p = paymentDAO.findById(paymentId);
        if (p == null) return "Payment not found.";
        if (p.getPaymentStatus().isFinal()) {
            return "This payment was already " + p.getPaymentStatus().label().toLowerCase() + ".";
        }
        paymentDAO.updateStatus(paymentId, PaymentStatus.REJECTED.name());
        activity.log("REJECTED", "Payment of " + peso(p.getAmount()) + " rejected for " + p.getTenantName());
        return "Payment rejected.";
    }
}