package boardinghouse.model;

public class Payment {
    private int paymentId;
    private int billId;
    private double amount;
    private String method;
    private String refNo;
    private String proofPath;
    private String status = "PENDING";
    private String paidAt;
    private String tenantName;
    private String billType;
    private String billingMonth;

    // new payment
    public Payment(int billId, double amount, String method, String refNo) {
        this.billId = billId;
        this.amount = amount;
        this.method = method;
        this.refNo = refNo;
    }

    // loaded from the database
    public Payment(int paymentId, int billId, double amount, String method, String refNo, String status, String paidAt) {
        this(billId, amount, method, refNo);
        this.paymentId = paymentId;
        this.status = status;
        this.paidAt = paidAt;
    }

    public int getPaymentId() { return paymentId; }
    public int getBillId() { return billId; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getRefNo() { return refNo; }
    public String getProofPath() { return proofPath; }
    public void setProofPath(String proofPath) { this.proofPath = proofPath; }
    public boolean hasProof() { return proofPath != null && !proofPath.isBlank(); }
    public String getStatus() { return status; }
    public String getPaidAt() { return paidAt; }
    public String getTenantName() { return tenantName; }
    public String getBillType() { return billType; }
    public String getBillingMonth() { return billingMonth; }

    public void setDisplayInfo(String tenantName, String billType, String billingMonth) {
        this.tenantName = tenantName;
        this.billType = billType;
        this.billingMonth = billingMonth;
    }
}