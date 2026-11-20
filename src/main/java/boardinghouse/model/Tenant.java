package boardinghouse.model;

public class Tenant {
    private int tenantId;
    private int accountId;
    private Integer roomId;
    private String name, address, occupation, emergencyNo;
    private int age;

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
}