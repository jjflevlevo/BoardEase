package boardinghouse.model;

public class Tenant {
    private int tenantId;
    private int accountId;
    private Integer roomId;
    private final String name;
    private final String address;
    private final int age;
    private final String occupation;
    private final String emergencyNo;
    private TenantStatus status = TenantStatus.ACTIVE;
    private String movedOutOn;

    public Tenant(String name, String address, int age, String occupation, String emergencyNo) {
        this.name = name;
        this.address = address;
        this.age = age;
        this.occupation = occupation;
        this.emergencyNo = emergencyNo;
    }

    public int getTenantId() { return tenantId; }
    public void setTenantId(int tenantId) { this.tenantId = tenantId; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public int getAge() { return age; }
    public String getOccupation() { return occupation; }
    public String getEmergencyNo() { return emergencyNo; }
    public String getMovedOutOn() { return movedOutOn; }
    public void setMovedOutOn(String movedOutOn) { this.movedOutOn = movedOutOn; }

    // the status as text ("ACTIVE" or "MOVED_OUT"), used by the database
    public String getStatus() { return status.name(); }
    public TenantStatus getTenantStatus() { return status; }
    public boolean isMovedOut() { return status == TenantStatus.MOVED_OUT; }

    public void setStatus(String text) {
        this.status = text == null || text.isBlank() ? TenantStatus.ACTIVE : TenantStatus.from(text);
    }
}