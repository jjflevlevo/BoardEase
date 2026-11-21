package boardinghouse.model;

public enum RequestStatus implements Labeled {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() { return label; }

    // upper-case text for the status pills, e.g. IN PROGRESS
    public String display() {
        return name().replace('_', ' ');
    }

    public static RequestStatus from(String text) {
        return Labeled.parse(RequestStatus.class, text);
    }
}