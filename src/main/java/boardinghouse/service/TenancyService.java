package boardinghouse.service;

import boardinghouse.dao.*;
import boardinghouse.model.Bill;
import boardinghouse.model.Deposit;
import boardinghouse.model.Payment;
import boardinghouse.model.Tenant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// security deposits, moving out, and moving back in
public class TenancyService {
    public record MoveOutPreview(double unpaid, double deposit, double deduction, double refund, double remainingDebt) { }

    public record Result(boolean ok, String message) { }

    private final TenantDAO tenantDAO = new TenantDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final DepositDAO depositDAO = new DepositDAO();
    private final PaymentService payments = new PaymentService();
    private final ActivityDAO activity = new ActivityDAO();

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String peso(double v) {
        return String.format("\u20B1%,.2f", v);
    }

    public Deposit depositOf(int tenantId) {
        return depositDAO.findByTenant(tenantId);
    }

    public double unpaidBalance(int tenantId) {
        double total = 0;
        for (Bill b : billDAO.findByTenant(tenantId)) total += payments.balanceOf(b);
        return round2(total);
    }

    // ---------- Deposits ----------
    // returns null when saved, otherwise what is wrong
    public String recordDeposit(Tenant tenant, double amount, LocalDate receivedOn, String note) {
        if (tenant == null) return "Tenant not found.";
        if (tenant.isMovedOut()) return "This tenant has already moved out.";
        if (amount <= 0) return "The deposit must be greater than 0.";
        if (amount > 1_000_000) return "The deposit amount is too large.";
        if (receivedOn == null) return "Enter the date the deposit was received.";
        if (receivedOn.isAfter(LocalDate.now())) return "The date cannot be in the future.";
        if (note != null && note.length() > 200) return "The note is too long (max 200 characters).";

        depositDAO.save(tenant.getTenantId(), round2(amount), receivedOn.toString(),
                note == null || note.isBlank() ? null : note);
        activity.log("SYSTEM", "Deposit of " + peso(amount) + " recorded for " + tenant.getName());
        return null;
    }

    // ---------- Moving out ----------
    // what will happen to the deposit if this tenant moves out now
    public MoveOutPreview preview(Tenant tenant) {
        double unpaid = unpaidBalance(tenant.getTenantId());
        Deposit d = depositDAO.findByTenant(tenant.getTenantId());
        double held = d != null && d.isHeld() ? d.getAmount() : 0;
        double deduction = Math.min(held, unpaid);
        return new MoveOutPreview(round2(unpaid), round2(held), round2(deduction),
                round2(held - deduction), round2(unpaid - deduction));
    }

    public Result moveOut(Tenant tenant) {
        if (tenant == null) return new Result(false, "Tenant not found.");
        if (tenant.isMovedOut()) return new Result(false, "This tenant has already moved out.");
        for (Payment p : paymentDAO.findByTenant(tenant.getTenantId())) {
            if ("PENDING".equals(p.getStatus())) {
                return new Result(false, "Approve or reject " + tenant.getName() + "'s pending payments first.");
            }
        }

        MoveOutPreview preview = preview(tenant);

        // the deposit pays the unpaid bills, oldest due date first
        double left = preview.deduction();
        List<Bill> bills = billDAO.findByTenant(tenant.getTenantId());
        bills.sort(Comparator.comparing(Bill::getDueDate));
        for (Bill b : bills) {
            if (left < 0.005) break;
            double balance = payments.balanceOf(b);
            if (balance <= 0) continue;
            double pay = round2(Math.min(balance, left));
            payFromDeposit(b, pay);
            left = round2(left - pay);
        }

        String today = LocalDate.now().toString();
        if (preview.deposit() > 0) depositDAO.settle(tenant.getTenantId(), preview.deduction(), preview.refund(), today);
        tenantDAO.moveOut(tenant.getTenantId(), today);
        activity.log("TENANT", tenant.getName() + " moved out"
                + (preview.deposit() > 0 ? " (deposit refund: " + peso(preview.refund()) + ")" : ""));

        StringBuilder sb = new StringBuilder(tenant.getName() + " has moved out.\n");
        if (preview.deposit() > 0) {
            sb.append("\nDeposit held: ").append(peso(preview.deposit()));
            sb.append("\nUsed for unpaid bills: ").append(peso(preview.deduction()));
            sb.append("\nRefund to give the tenant: ").append(peso(preview.refund()));
        }
        if (preview.remainingDebt() > 0) {
            sb.append("\n\nStill unpaid after the deposit: ").append(peso(preview.remainingDebt()));
        }
        return new Result(true, sb.toString());
    }

    // records part of the deposit as an approved payment on a bill
    private void payFromDeposit(Bill bill, double amount) {
        paymentDAO.insert(new Payment(bill.getBillId(), amount, "DEPOSIT", "Security deposit"));
        Payment latest = null;
        for (Payment p : paymentDAO.findPending()) {
            if (p.getBillId() == bill.getBillId() && (latest == null || p.getPaymentId() > latest.getPaymentId())) {
                latest = p;
            }
        }
        if (latest != null) payments.approve(latest.getPaymentId());
    }

    public Result restore(Tenant tenant) {
        if (tenant == null) return new Result(false, "Tenant not found.");
        if (!tenant.isMovedOut()) return new Result(false, "This tenant is already active.");
        tenantDAO.restore(tenant.getTenantId());
        activity.log("TENANT", tenant.getName() + " was restored as an active tenant");
        return new Result(true, tenant.getName() + " is active again and can log in. "
                + "Assign a room when they move back in.");
    }
}