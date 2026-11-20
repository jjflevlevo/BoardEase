package boardinghouse.model;

import java.time.LocalDate;

public abstract class Bill {
    public static final double LATE_FEE_RATE = 0.05; // 5% of the bill, charged once when overdue

    protected int billId;
    protected int tenantId;
    protected String billingMonth;
    protected LocalDate dueDate;
    protected String status = "UNPAID";
    protected double amount;
    protected String tenantName;

    // for a new bill
    protected Bill(int tenantId, String billingMonth, LocalDate dueDate) {
        this.tenantId = tenantId;
        this.billingMonth = billingMonth;
        this.dueDate = dueDate;
    }

    // for a bill loaded from the database
    protected Bill(int billId, int tenantId, String billingMonth, LocalDate dueDate, String status, double amount) {
        this(tenantId, billingMonth, dueDate);
        this.billId = billId;
        this.status = status;
        this.amount = amount;
    }

    public abstract double computeAmount();
    public abstract String getType();

    public double getLateFee(LocalDate today) {
        if ("PAID".equals(status) || !today.isAfter(dueDate)) return 0;
        return round2(amount * LATE_FEE_RATE);
    }

    protected static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public int getBillId() { return billId; }
    public int getTenantId() { return tenantId; }
    public String getBillingMonth() { return billingMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}