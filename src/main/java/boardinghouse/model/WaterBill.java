package boardinghouse.model;

import java.time.LocalDate;

public class WaterBill extends Bill {
    private double usage;     // cubic meters used by the whole room
    private double rate;      // price per cubic meter
    private int occupants;    // tenants sharing the meter

    public WaterBill(int tenantId, String billingMonth, LocalDate dueDate, double usage, double rate, int occupants) {
        super(tenantId, billingMonth, dueDate);
        this.usage = usage;
        this.rate = rate;
        this.occupants = occupants;
        this.amount = computeAmount();
    }

    public WaterBill(int billId, int tenantId, String billingMonth, LocalDate dueDate, String status, double amount) {
        super(billId, tenantId, billingMonth, dueDate, status, amount);
    }

    @Override
    public double computeAmount() { return round2(usage * rate / occupants); }

    @Override
    public String getType() { return "WATER"; }
}
