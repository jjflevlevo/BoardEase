package boardinghouse.model;

import java.time.LocalDate;

public class ElectricBill extends Bill {
    private double kwh;       // kWh used by the whole room (submeter)
    private double rate;      // price per kWh
    private int occupants;    // tenants sharing the meter

    public ElectricBill(int tenantId, String billingMonth, LocalDate dueDate, double kwh, double rate, int occupants) {
        super(tenantId, billingMonth, dueDate);
        this.kwh = kwh;
        this.rate = rate;
        this.occupants = occupants;
        this.amount = computeAmount();
    }

    public ElectricBill(int billId, int tenantId, String billingMonth, LocalDate dueDate, String status, double amount) {
        super(billId, tenantId, billingMonth, dueDate, status, amount);
    }

    @Override
    public double computeAmount() { return round2(kwh * rate / occupants); }

    @Override
    public String getType() { return "ELECTRIC"; }
}