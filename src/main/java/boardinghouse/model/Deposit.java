package boardinghouse.model;

public class Deposit {
    private final int depositId;
    private final int tenantId;
    private final double amount;
    private final String receivedOn;
    private final DepositStatus status;
    private final double deducted; // used for unpaid bills at move-out
    private final double refunded; // given back to the tenant at move-out
    private final String settledOn;
    private final String note;
    private String tenantName;
    private String roomNo;

    public Deposit(int depositId, int tenantId, double amount, String receivedOn, String status,
                   double deducted, double refunded, String settledOn, String note) {
        this.depositId = depositId;
        this.tenantId = tenantId;
        this.amount = amount;
        this.receivedOn = receivedOn;
        this.status = DepositStatus.from(status);
        this.deducted = deducted;
        this.refunded = refunded;
        this.settledOn = settledOn;
        this.note = note;
    }

    public int getDepositId() { return depositId; }
    public int getTenantId() { return tenantId; }
    public double getAmount() { return amount; }
    public String getReceivedOn() { return receivedOn; }
    public double getDeducted() { return deducted; }
    public double getRefunded() { return refunded; }
    public String getSettledOn() { return settledOn; }
    public String getNote() { return note; }
    public String getTenantName() { return tenantName; }
    public String getRoomNo() { return roomNo; }

    // the status as text ("HELD" or "RETURNED"), used by the screens
    public String getStatus() { return status.name(); }
    public DepositStatus getDepositStatus() { return status; }
    public boolean isHeld() { return status == DepositStatus.HELD; }
    public boolean isReturned() { return status == DepositStatus.RETURNED; }

    public void setDisplayInfo(String tenantName, String roomNo) {
        this.tenantName = tenantName;
        this.roomNo = roomNo;
    }
}