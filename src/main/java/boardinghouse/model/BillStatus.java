package boardinghouse.model;

public enum BillStatus implements Labeled {
    UNPAID("Unpaid"),
    PARTIAL("Partially paid"),
    PAID("Paid");

    private final String label;

    BillStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    // decides the status from how much has been paid so far
    public static BillStatus fromAmounts(double paid, double total) {
        if (paid + 0.005 >= total) return PAID;
        return paid > 0 ? PARTIAL : UNPAID;
    }

    public static BillStatus from(String text) {
        return Labeled.parse(BillStatus.class, text);
    }
}