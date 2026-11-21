package boardinghouse.model;

public class Payment {
    private int paymentId;
    private final int billId;
    private final double amount;
    private final PaymentMethod method;
    private final String refNo;
    private String proofPath;
    private PaymentStatus status = PaymentStatus.PENDING;
    private String paidAt;
    private String tenantName;
    private String billType;
    private String billingMonth;

    // Overload 1: new payment with the method as an enum
    public Payment(int billId, double amount, PaymentMethod method, String refNo) {
        this.billId = billId;
        this.amount = amount;
        this.method = method;
        this.refNo = refNo;
    }

    // Overload 2: new payment with the method as text ("CASH", "GCASH", "DEPOSIT")
    public Payment(int billId, double amount, String method, String refNo) {
        this(billId, amount, PaymentMethod.from(method), refNo);
    }

    // Overload 3: loaded from the database
    public Payment(int paymentId, int billId, double amount, String method, String refNo, String status, String paidAt) {
        this(billId, amount, method, refNo);
        this.paymentId = paymentId;
        this.status = PaymentStatus.from(status);
        this.paidAt = paidAt;
    }

    public int getPaymentId() { return paymentId; }
    public int getBillId() { return billId; }
    public double getAmount() { return amount; }
    public String getRefNo() { return refNo; }
    public String getProofPath() { return proofPath; }
    public void setProofPath(String proofPath) { this.proofPath = proofPath; }
    public boolean hasProof() { return proofPath != null && !proofPath.isBlank(); }
    public String getPaidAt() { return paidAt; }
    public String getTenantName() { return tenantName; }
    public String getBillType() { return billType; }
    public String getBillingMonth() { return billingMonth; }

    // method and status as text, used by the screens and the database
    public String getMethod() { return method.name(); }
    public String getStatus() { return status.name(); }
    public PaymentMethod getPaymentMethod() { return method; }
    public PaymentStatus getPaymentStatus() { return status; }
    public boolean isPending() { return status == PaymentStatus.PENDING; }
    public boolean isApproved() { return status == PaymentStatus.APPROVED; }

    public void setDisplayInfo(String tenantName, String billType, String billingMonth) {
        this.tenantName = tenantName;
        this.billType = billType;
        this.billingMonth = billingMonth;
    }
}