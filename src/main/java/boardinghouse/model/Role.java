package boardinghouse.model;

public enum Role implements Labeled {
    OWNER("Owner"),
    TENANT("Tenant");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    public static Role from(String text) {
        return Labeled.parse(Role.class, text);
    }
}