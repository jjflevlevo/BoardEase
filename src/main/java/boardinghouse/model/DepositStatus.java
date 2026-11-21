package boardinghouse.model;

public enum DepositStatus implements Labeled {
    HELD("Held"),
    RETURNED("Returned");

    private final String label;

    DepositStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    public static DepositStatus from(String text) {
        return Labeled.parse(DepositStatus.class, text);
    }
}