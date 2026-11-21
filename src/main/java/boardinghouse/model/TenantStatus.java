package boardinghouse.model;

public enum TenantStatus implements Labeled {
    ACTIVE("Active"),
    MOVED_OUT("Moved out");

    private final String label;

    TenantStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    public static TenantStatus from(String text) {
        return Labeled.parse(TenantStatus.class, text);
    }
}