package boardinghouse.model;

public class MaintenanceRequest {
    private final int requestId;
    private final int tenantId;
    private final Integer roomId;
    private final String category;
    private final String description;
    private final RequestStatus status;
    private final String ownerNote;
    private final String createdAt;
    private final String updatedAt;
    private String tenantName;
    private String roomNo;

    public MaintenanceRequest(int requestId, int tenantId, Integer roomId, String category, String description,
                              String status, String ownerNote, String createdAt, String updatedAt) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.category = category;
        this.description = description;
        this.status = RequestStatus.from(status);
        this.ownerNote = ownerNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getRequestId() { return requestId; }
    public int getTenantId() { return tenantId; }
    public Integer getRoomId() { return roomId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getOwnerNote() { return ownerNote; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getTenantName() { return tenantName; }
    public String getRoomNo() { return roomNo; }

    // the status as text ("OPEN", "IN_PROGRESS", "DONE"), used by the screens
    public String getStatus() { return status.name(); }
    public RequestStatus getRequestStatus() { return status; }
    public String getStatusLabel() { return status.display(); }
    public boolean isOpen() { return status == RequestStatus.OPEN; }
    public boolean isDone() { return status == RequestStatus.DONE; }

    public void setDisplayInfo(String tenantName, String roomNo) {
        this.tenantName = tenantName;
        this.roomNo = roomNo;
    }
}