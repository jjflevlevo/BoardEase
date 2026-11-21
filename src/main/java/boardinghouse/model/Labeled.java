package boardinghouse.model;

// an enum value that has a friendly name for the screen
public interface Labeled {
    String label();

    // turns text saved in the database (like "PAID") back into the enum value
    static <E extends Enum<E>> E parse(Class<E> type, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Missing value for " + type.getSimpleName());
        }
        return Enum.valueOf(type, text.trim().toUpperCase());
    }
}