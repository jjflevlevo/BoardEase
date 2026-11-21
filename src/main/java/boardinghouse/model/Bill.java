package boardinghouse.model;

import java.time.LocalDate;

// a bill is abstract: RentBill, WaterBill, and ElectricBill each compute their own amount
public abstract class Bill implements Chargeable {
    public static final double LATE_FEE_RATE = 0.05; // 5% of the bill, charged once when overdue

    protected int billId;
    protected int tenantId;
    protected String billingMonth;
    protected LocalDate dueDate;
    protected BillStatus status = BillStatus.UNPAID;
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
        this.status = BillStatus.from(status);
        this.amount = amount;
    }

    @Override
    public abstract double computeAmount();

    @Override
    public abstract String getType();

    @Override
    public double getLateFee(LocalDate today) {
        if (isPaid() || !today.isAfter(dueDate)) return 0;
        return round2(amount * LATE_FEE_RATE);
    }

    protected static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public int getBillId() { return billId; }
    public int getTenantId() { return tenantId; }
    public String getBillingMonth() { return billingMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public double getAmount() { return amount; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    // the status as text ("UNPAID", "PARTIAL", "PAID"), used by the screens
    public String getStatus() { return status.name(); }
    public BillStatus getBillStatus() { return status; }
    public boolean isPaid() { return status == BillStatus.PAID; }
}