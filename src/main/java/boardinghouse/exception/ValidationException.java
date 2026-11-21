package boardinghouse.exception;

// thrown when typed-in information is missing or not valid
public class ValidationException extends BoardEaseException {
    public ValidationException(String message) {
        super(message);
    }
}