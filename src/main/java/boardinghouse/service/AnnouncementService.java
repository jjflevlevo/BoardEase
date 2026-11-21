package boardinghouse.service;

import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.AnnouncementDAO;

public class AnnouncementService {
    public static final int MAX_TITLE = 80;
    public static final int MAX_MESSAGE = 1000;

    private final AnnouncementDAO dao = new AnnouncementDAO();
    private final ActivityDAO activity = new ActivityDAO();

    // returns null when posted, otherwise what is wrong
    public String post(String title, String message, boolean important) {
        String error = validate(title, message);
        if (error != null) return error;
        if (!dao.insert(title, message, important)) return "Could not post the announcement. Try again.";
        activity.log("SYSTEM", "Announcement posted: " + title);
        return null;
    }

    public String edit(int id, String title, String message, boolean important) {
        String error = validate(title, message);
        if (error != null) return error;
        return dao.update(id, title, message, important) ? null : "Announcement not found.";
    }

    public void remove(int id) {
        dao.delete(id);
    }

    private String validate(String title, String message) {
        if (title == null || title.isBlank()) return "Enter a title.";
        if (title.length() > MAX_TITLE) return "The title is too long (max " + MAX_TITLE + " characters).";
        if (message == null || message.isBlank()) return "Enter the message.";
        if (message.length() > MAX_MESSAGE) return "The message is too long (max " + MAX_MESSAGE + " characters).";
        return null;
    }
}