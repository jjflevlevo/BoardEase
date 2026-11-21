package boardinghouse.model;

public enum PaymentMethod implements Labeled {
    CASH("Cash", false),
    GCASH("GCash", true),
    DEPOSIT("Security deposit", false);

    private final String label;
    private final boolean online;

    PaymentMethod(String label, boolean online) {
        this.label = label;
        this.online = online;
    }

    @Override
    public String label() { return label; }

    // online payments need the owner's GCash details and a reference number
    public boolean isOnline() { return online; }

    public static PaymentMethod from(String text) {
        return Labeled.parse(PaymentMethod.class, text);
    }
}