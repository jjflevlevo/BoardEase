package boardinghouse.exception;

// thrown when a payment cannot be accepted (too high, missing reference no., bill already paid, and so on)
public class InvalidPaymentException extends BoardEaseException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}