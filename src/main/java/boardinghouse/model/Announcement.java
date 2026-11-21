package boardinghouse.model;

public class Announcement {
    private final int announcementId;
    private final String title;
    private final String message;
    private final boolean important;
    private final String createdAt;

    public Announcement(int announcementId, String title, String message, boolean important, String createdAt) {
        this.announcementId = announcementId;
        this.title = title;
        this.message = message;
        this.important = important;
        this.createdAt = createdAt;
    }

    public int getAnnouncementId() { return announcementId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isImportant() { return important; }
    public String getCreatedAt() { return createdAt; }
}