package boardinghouse.service;

import boardinghouse.dao.*;
import boardinghouse.model.Bill;
import boardinghouse.model.Payment;
import boardinghouse.model.Room;
import boardinghouse.model.Tenant;
import boardinghouse.util.DatabaseConnection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// fills an empty system with sample data for demos and testing
public class DemoDataService {
    public static final String DEMO_PASSWORD = "tenant123";
    private static final double WATER_RATE = 30;
    private static final double ELECTRIC_RATE = 12;

    private final RoomDAO roomDAO = new RoomDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MeterReadingDAO meterDAO = new MeterReadingDAO();
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final SettingsDAO settings = new SettingsDAO();
    private final AuthService auth = new AuthService();
    private final BillingService billing = new BillingService();
    private final PaymentService payments = new PaymentService();
    private final TenancyService tenancy = new TenancyService();
    private final AnnouncementService announcements = new AnnouncementService();
    private final MaintenanceService maintenance = new MaintenanceService();

    public boolean isEmpty() {
        return roomDAO.findAll().isEmpty() && tenantDAO.findEveryone().isEmpty();
    }

    public String load() {
        if (!isEmpty()) return "Demo data can only be loaded when there are no rooms and no tenants yet.";

        // rooms
        roomDAO.insert("101", 2, 1500);
        roomDAO.insert("102", 2, 1500);
        roomDAO.insert("103", 3, 1200);
        roomDAO.insert("104", 2, 1800);
        Map<String, Room> rooms = new HashMap<>();
        for (Room r : roomDAO.findAll()) rooms.put(r.getRoomNo(), r);

        // GCash details, so online payments work
        settings.saveGcash("BoardEase Owner", "09171234567");

        // name, email, phone, address, age, occupation, emergency no., room
        String[][] people = {
                {"Maria Santos", "maria.santos@demo.com", "09171112222", "Daraga, Albay", "20", "Nursing student", "09181112222", "101"},
                {"Juan Dela Cruz", "juan.delacruz@demo.com", "09173334444", "Sorsogon City", "21", "IT student", "09183334444", "101"},
                {"Ana Reyes", "ana.reyes@demo.com", "09175556666", "Tabaco, Albay", "24", "Call center agent", "09185556666", "102"},
                {"Mark Villanueva", "mark.villanueva@demo.com", "09177778888", "Ligao, Albay", "19", "Engineering student", "09187778888", "103"},
                {"Carla Mendoza", "carla.mendoza@demo.com", "09179990000", "Naga City", "27", "Bank teller", "09189990000", "103"},
                {"Paolo Garcia", "paolo.garcia@demo.com", "09170001111", "Legazpi City", "23", "Sales associate", "09180001111", "104"}};

        YearMonth now = YearMonth.now();
        YearMonth[] months = {now.minusMonths(3), now.minusMonths(2), now.minusMonths(1), now};

        List<Tenant> created = new ArrayList<>();
        Map<String, Tenant> byFirstName = new HashMap<>();
        for (String[] p : people) {
            Tenant t = new Tenant(p[0], p[3], Integer.parseInt(p[4]), p[5], p[6]);
            if (!auth.signUp(p[1], p[2], DEMO_PASSWORD, t)) continue;
            Tenant saved = tenantDAO.findByAccountId(accountDAO.findByEmail(p[1]).getAccountId());
            Room room = rooms.get(p[7]);
            tenantDAO.assignRoom(saved.getTenantId(), room.getRoomId());
            saved = tenantDAO.findById(saved.getTenantId());
            tenancy.recordDeposit(saved, room.getMonthlyRent(), months[0].atDay(1), "Paid in cash");
            created.add(saved);
            byFirstName.put(p[0].split(" ")[0], saved);
        }
        if (created.isEmpty()) return "Could not create the demo tenants. Their emails may already be registered.";

        // meter readings: a starting reading 3 months ago, then one per month up to this month
        for (Room r : rooms.values()) {
            double electric = 1000 + r.getRoomId() * 250;
            double water = 100 + r.getRoomId() * 20;
            for (int i = 0; i < months.length; i++) {
                if (i > 0) {
                    electric += 70 + i * 12 + r.getRoomId() * 6;
                    water += 7 + i + r.getRoomId();
                }
                meterDAO.insert(r.getRoomId(), "ELECTRIC", electric, months[i].toString());
                meterDAO.insert(r.getRoomId(), "WATER", water, months[i].toString());
            }
        }

        // bills for the last 3 months, each due at the end of its month
        for (int i = 1; i < months.length; i++) {
            billing.generateBills(months[i].toString(), months[i].atEndOfMonth(), WATER_RATE, ELECTRIC_RATE);
        }

        // 2 months ago everyone paid; last month Ana paid nothing and Mark paid part of his rent;
        // this month Maria sent a GCash payment that waits for approval, and Paolo paid his rent
        String twoAgo = months[1].toString();
        String lastMonth = months[2].toString();
        String thisMonth = months[3].toString();
        for (Tenant t : created) {
            String first = t.getName().split(" ")[0];
            for (Bill b : billDAO.findByTenant(t.getTenantId())) {
                String month = b.getBillingMonth();
                if (month.equals(twoAgo)) {
                    payAndApprove(b, first.equals("Ana") ? "GCASH" : "CASH", months[1].atEndOfMonth().minusDays(5));
                } else if (month.equals(lastMonth) && !first.equals("Ana")) {
                    if (first.equals("Mark")) {
                        if ("RENT".equals(b.getType())) payAndApprove(b, 1000, "CASH", months[2].atEndOfMonth().minusDays(3));
                    } else {
                        payAndApprove(b, "CASH", months[2].atEndOfMonth().minusDays(4));
                    }
                } else if (month.equals(thisMonth) && "RENT".equals(b.getType())) {
                    if (first.equals("Maria")) {
                        payments.submitPayment(t.getTenantId(), b.getBillId(), b.getAmount(), "GCASH", "1029384756");
                    } else if (first.equals("Paolo")) {
                        payAndApprove(b, "CASH", LocalDate.now());
                    }
                }
            }
        }

        // announcements
        announcements.post("Water interruption on Saturday",
                "The water district will cut the water supply this Saturday from 8 AM to 5 PM.\n"
                        + "Please store enough water the night before.", true);
        announcements.post("Rent reminder",
                "Rent for this month is due at the end of the month. You can pay in cash or through GCash. Thank you!",
                false);

        // maintenance requests: one open, one in progress, one done
        int requests = 0;
        requests += request(byFirstName.get("Maria"), "Plumbing",
                "The faucet in the bathroom keeps leaking even when it is fully closed.", null, null);
        requests += request(byFirstName.get("Mark"), "Electrical",
                "The light in our room flickers and sometimes turns off by itself.",
                "IN_PROGRESS", "An electrician will check it on Saturday morning.");
        requests += request(byFirstName.get("Juan"), "Internet",
                "The Wi-Fi signal is very weak in room 101 at night.",
                "DONE", "The router was moved closer to your room. Tell me if it is still slow.");

        // Paolo moves out: his deposit pays his unpaid utilities, and the rest is refunded
        Tenant paolo = byFirstName.get("Paolo");
        boolean movedOut = paolo != null && tenancy.moveOut(tenantDAO.findById(paolo.getTenantId())).ok();

        int bills = billDAO.findAll().size();
        int approved = paymentDAO.findByStatus("APPROVED").size();
        int pending = paymentDAO.findPending().size();
        return "Demo data loaded:\n"
                + rooms.size() + " rooms, " + created.size() + " tenants, " + bills + " bills, "
                + approved + " approved and " + pending + " pending payment(s),\n"
                + "deposits, 2 announcements, " + requests + " maintenance requests"
                + (movedOut ? ", and 1 tenant who moved out (Paolo Garcia)." : ".") + "\n\n"
                + "Every demo tenant logs in with the password " + DEMO_PASSWORD + ", for example:\n"
                + "maria.santos@demo.com";
    }

    // returns 1 when the request was created
    private int request(Tenant tenant, String category, String description, String status, String note) {
        if (tenant == null) return 0;
        if (maintenance.submit(tenantDAO.findById(tenant.getTenantId()), category, description) != null) return 0;
        if (status != null) {
            int id = maintenanceDAO.findByTenant(tenant.getTenantId()).get(0).getRequestId();
            maintenance.updateStatus(id, status, note);
        }
        return 1;
    }

    // Overload 1: pays the whole remaining balance
    private void payAndApprove(Bill bill, String method, LocalDate paidOn) {
        payAndApprove(bill, payments.balanceOf(bill), method, paidOn);
    }

    // Overload 2: pays a specific amount
    private void payAndApprove(Bill bill, double amount, String method, LocalDate paidOn) {
        String ref = "GCASH".equals(method) ? String.format("88%08d", bill.getBillId() * 7919) : null;
        String result = payments.submitPayment(bill.getTenantId(), bill.getBillId(), amount, method, ref);
        if (!result.startsWith("Payment submitted")) return;

        Payment latest = null;
        for (Payment p : paymentDAO.findPending()) {
            if (p.getBillId() == bill.getBillId() && (latest == null || p.getPaymentId() > latest.getPaymentId())) {
                latest = p;
            }
        }
        if (latest == null) return;
        payments.approve(latest.getPaymentId());
        backdate(latest.getPaymentId(), paidOn);
    }

    // sample payments get past dates, so the income chart shows several months
    private void backdate(int paymentId, LocalDate date) {
        String sql = "UPDATE payments SET paid_at = ? WHERE payment_id = ?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, date + " 04:00:00");
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        } catch (SQLException ignored) { }
    }
}