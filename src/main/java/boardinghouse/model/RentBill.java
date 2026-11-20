package boardinghouse.model;

import java.time.LocalDate;

public class RentBill extends Bill {
    private double rate;

    public RentBill(int tenantId, String billingMonth, LocalDate dueDate, double rate) {
        super(tenantId, billingMonth, dueDate);
        this.rate = rate;
        this.amount = computeAmount();
    }

    public RentBill(int billId, int tenantId, String billingMonth, LocalDate dueDate, String status, double amount) {
        super(billId, tenantId, billingMonth, dueDate, status, amount);
    }

    @Override
    public double computeAmount() { return round2(rate); }

    @Override
    public String getType() { return "RENT"; }
}