package boardinghouse.model;

public enum PaymentStatus implements Labeled {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    // an approved or rejected payment can no longer be changed
    public boolean isFinal() {
        return this != PENDING;
    }

    public static PaymentStatus from(String text) {
        return Labeled.parse(PaymentStatus.class, text);
    }
}