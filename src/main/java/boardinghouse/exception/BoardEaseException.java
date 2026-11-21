package boardinghouse.exception;

// base class for problems the user can fix; the message is shown on screen
public class BoardEaseException extends Exception {
    public BoardEaseException(String message) {
        super(message);
    }
}