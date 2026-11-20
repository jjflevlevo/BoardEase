package boardinghouse.ui;

import boardinghouse.dao.*;
import boardinghouse.model.*;
import boardinghouse.service.BillingService;
import boardinghouse.service.PaymentService;
import boardinghouse.util.InputHelper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class OwnerMenu implements Menu {
    private final Account account;
    private final RoomDAO roomDAO = new RoomDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final MeterReadingDAO meterDAO = new MeterReadingDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final BillingService billing = new BillingService();
    private final PaymentService payments = new PaymentService();

    public OwnerMenu(Account account) { this.account = account; }

    @Override
    public void show() {
        while (true) {
            int pending = paymentDAO.findPending().size();
            System.out.println("\n=== Owner Menu (" + account.getEmail() + ") ===");
            System.out.println("1. Manage rooms\n2. Manage tenants\n3. Billing");
            System.out.println("4. Payments" + (pending > 0 ? "  <-- " + pending + " pending approval" : ""));
            System.out.println("5. Reports\n6. Logout");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) manageRooms();
            else if (c == 2) manageTenants();
            else if (c == 3) billingMenu();
            else if (c == 4) reviewPayments();
            else if (c == 5) reportsMenu();
            else if (c == 6) return;
            else System.out.println("Invalid choice.");
        }
    }

    // ---------- Rooms ----------
    private void manageRooms() {
        while (true) {
            System.out.println("\n--- Rooms ---\n1. Add room\n2. List rooms\n3. Delete room\n4. Back");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) {
                String no = InputHelper.readString("Room no.: ");
                int cap = InputHelper.readInt("Capacity: ");
                double rent = InputHelper.readDouble("Monthly rent per tenant: ");
                if (no.isEmpty() || cap <= 0 || rent <= 0) System.out.println("Invalid values.");
                else System.out.println(roomDAO.insert(no, cap, rent) ? "Room added." : "Room no. already exists.");
            } else if (c == 2) listRooms();
            else if (c == 3) {
                listRooms();
                int id = InputHelper.readInt("Room ID to delete: ");
                System.out.println(roomDAO.delete(id) ? "Room deleted." : "Cannot delete (not found or still in use).");
            } else if (c == 4) return;
            else System.out.println("Invalid choice.");
        }
    }

    private void listRooms() {
        List<Room> rooms = roomDAO.findAll();
        if (rooms.isEmpty()) { System.out.println("No rooms yet."); return; }
        System.out.printf("%-4s %-8s %-10s %-10s %s%n", "ID", "Room", "Rent", "Capacity", "Occupied");
        for (Room r : rooms) {
            System.out.printf("%-4d %-8s %-10.2f %-10d %d%n", r.getRoomId(), r.getRoomNo(),
                    r.getMonthlyRent(), r.getCapacity(), tenantDAO.countInRoom(r.getRoomId()));
        }
    }

    // ---------- Tenants ----------
    private void manageTenants() {
        while (true) {
            System.out.println("\n--- Tenants ---\n1. List tenants\n2. Assign or transfer room\n3. Remove tenant from room\n4. Back");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) listTenants();
            else if (c == 2) assignRoom();
            else if (c == 3) removeFromRoom();
            else if (c == 4) return;
            else System.out.println("Invalid choice.");
        }
    }

    private void listTenants() {
        List<Tenant> tenants = tenantDAO.findAll();
        if (tenants.isEmpty()) { System.out.println("No tenants yet."); return; }
        System.out.printf("%-4s %-20s %s%n", "ID", "Name", "Room");
        for (Tenant t : tenants) {
            Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
            System.out.printf("%-4d %-20s %s%n", t.getTenantId(), t.getName(), r == null ? "Unassigned" : r.getRoomNo());
        }
    }

    private void assignRoom() {
        listTenants();
        int tenantId = InputHelper.readInt("Tenant ID: ");
        listRooms();
        int roomId = InputHelper.readInt("Room ID: ");
        Room room = roomDAO.findById(roomId);
        if (room == null) { System.out.println("Room not found."); return; }
        if (tenantDAO.countInRoom(roomId) >= room.getCapacity()) {
            System.out.println("Room is full.");
            return;
        }
        System.out.println(tenantDAO.assignRoom(tenantId, roomId) ? "Room assigned." : "Tenant not found.");
    }

    private void removeFromRoom() {
        listTenants();
        int tenantId = InputHelper.readInt("Tenant ID to remove from their room: ");
        Tenant target = tenantDAO.findAll().stream().filter(t -> t.getTenantId() == tenantId).findFirst().orElse(null);
        if (target == null) { System.out.println("Tenant not found."); return; }
        if (target.getRoomId() == null) { System.out.println("That tenant has no room."); return; }

        double debt = debtOf(target);
        if (debt > 0) {
            System.out.printf("Cannot remove. %s still owes %.2f.%n", target.getName(), debt);
            return;
        }
        System.out.println(tenantDAO.assignRoom(tenantId, null) ? "Tenant removed from the room." : "Update failed.");
    }

    private double debtOf(Tenant t) {
        double total = 0;
        for (Bill b : billDAO.findByTenant(t.getTenantId())) total += payments.balanceOf(b);
        return total;
    }

    // ---------- Billing ----------
    private void billingMenu() {
        while (true) {
            System.out.println("\n--- Billing ---\n1. Enter meter reading\n2. Generate monthly bills\n3. View all bills\n4. Back");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) enterReading();
            else if (c == 2) generateBills();
            else if (c == 3) listBills();
            else if (c == 4) return;
            else System.out.println("Invalid choice.");
        }
    }

    private void enterReading() {
        listRooms();
        int roomId = InputHelper.readInt("Room ID: ");
        if (roomDAO.findById(roomId) == null) { System.out.println("Room not found."); return; }

        System.out.println("1. Water (cubic meters)\n2. Electric (kWh)");
        int t = InputHelper.readInt("Type: ");
        if (t != 1 && t != 2) { System.out.println("Invalid type."); return; }
        String type = t == 1 ? "WATER" : "ELECTRIC";

        String month = readMonth();
        if (month == null) return;

        double reading = InputHelper.readDouble("Meter reading: ");
        if (reading < 0) { System.out.println("Reading cannot be negative."); return; }

        Double previous = meterDAO.findPrevious(roomId, type, month);
        if (previous != null && reading < previous) {
            System.out.println("Reading cannot be lower than the previous one (" + previous + ").");
            return;
        }
        System.out.println(meterDAO.insert(roomId, type, reading, month)
                ? "Reading saved." : "A reading for that room, type, and month already exists.");
    }

    private void generateBills() {
        String month = readMonth();
        if (month == null) return;

        LocalDate due;
        try {
            due = LocalDate.parse(InputHelper.readString("Due date (yyyy-MM-dd): "));
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        double waterRate = InputHelper.readDouble("Water rate per cubic meter: ");
        double electricRate = InputHelper.readDouble("Electric rate per kWh: ");
        if (waterRate < 0 || electricRate < 0) { System.out.println("Rates cannot be negative."); return; }

        listRooms();
        int roomId = InputHelper.readInt("Room ID (0 for all rooms): ");
        if (roomId == 0) {
            System.out.println(billing.generateBills(month, due, waterRate, electricRate));
        } else {
            Room room = roomDAO.findById(roomId);
            if (room == null) { System.out.println("Room not found."); return; }
            System.out.println(billing.generateBills(room, month, due, waterRate, electricRate));
        }
    }

    private void listBills() {
        List<Bill> bills = billDAO.findAll();
        if (bills.isEmpty()) { System.out.println("No bills yet."); return; }
        System.out.printf("%-4s %-18s %-9s %-9s %-10s %-10s %-11s %s%n",
                "ID", "Tenant", "Type", "Month", "Amount", "Balance", "Due", "Status");
        for (Bill b : bills) {
            System.out.printf("%-4d %-18s %-9s %-9s %-10.2f %-10.2f %-11s %s%n", b.getBillId(), b.getTenantName(),
                    b.getType(), b.getBillingMonth(), b.getAmount(), payments.balanceOf(b), b.getDueDate(), b.getStatus());
        }
    }

    // ---------- Payments ----------
    private void reviewPayments() {
        List<Payment> pending = paymentDAO.findPending();
        if (pending.isEmpty()) { System.out.println("No pending payments."); return; }

        System.out.println("\n--- Pending payments ---");
        System.out.printf("%-4s %-18s %-9s %-9s %-10s %-7s %s%n", "ID", "Tenant", "Bill", "Month", "Amount", "Method", "Ref no.");
        for (Payment p : pending) {
            System.out.printf("%-4d %-18s %-9s %-9s %-10.2f %-7s %s%n", p.getPaymentId(), p.getTenantName(),
                    p.getBillType(), p.getBillingMonth(), p.getAmount(), p.getMethod(),
                    p.getRefNo() == null ? "-" : p.getRefNo());
        }

        int id = InputHelper.readInt("Payment ID to review (0 to cancel): ");
        if (id == 0) return;
        System.out.println("1. Approve\n2. Reject\n3. Cancel");
        int c = InputHelper.readInt("Choose: ");
        if (c == 1) System.out.println(payments.approve(id));
        else if (c == 2) System.out.println(payments.reject(id));
    }

    // ---------- Reports ----------
    private void reportsMenu() {
        while (true) {
            System.out.println("\n--- Reports ---\n1. Overdue bills\n2. Monthly income\n3. Tenant balances\n4. Back");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) overdueReport();
            else if (c == 2) incomeReport();
            else if (c == 3) balancesReport();
            else if (c == 4) return;
            else System.out.println("Invalid choice.");
        }
    }

    private void overdueReport() {
        LocalDate today = LocalDate.now();
        List<Bill> overdue = billDAO.findAll().stream()
                .filter(b -> !"PAID".equals(b.getStatus()) && today.isAfter(b.getDueDate())).toList();
        if (overdue.isEmpty()) { System.out.println("No overdue bills."); return; }

        System.out.printf("%-4s %-18s %-9s %-9s %-11s %-6s %-9s %s%n",
                "ID", "Tenant", "Type", "Month", "Due", "Days", "Late fee", "Balance");
        for (Bill b : overdue) {
            System.out.printf("%-4d %-18s %-9s %-9s %-11s %-6d %-9.2f %.2f%n", b.getBillId(), b.getTenantName(),
                    b.getType(), b.getBillingMonth(), b.getDueDate(), ChronoUnit.DAYS.between(b.getDueDate(), today),
                    b.getLateFee(today), payments.balanceOf(b));
        }
    }

    private void incomeReport() {
        Map<String, Double> income = paymentDAO.incomeByMonth();
        if (income.isEmpty()) { System.out.println("No approved payments yet."); return; }
        System.out.printf("%-10s %s%n", "Month", "Income");
        for (Map.Entry<String, Double> e : income.entrySet()) {
            System.out.printf("%-10s %.2f%n", e.getKey(), e.getValue());
        }
    }

    private void balancesReport() {
        List<Tenant> tenants = tenantDAO.findAll();
        if (tenants.isEmpty()) { System.out.println("No tenants yet."); return; }
        System.out.printf("%-4s %-20s %-8s %s%n", "ID", "Name", "Room", "Remaining debt");
        double grand = 0;
        for (Tenant t : tenants) {
            Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
            double debt = debtOf(t);
            grand += debt;
            System.out.printf("%-4d %-20s %-8s %.2f%n", t.getTenantId(), t.getName(), r == null ? "-" : r.getRoomNo(), debt);
        }
        System.out.printf("Total unpaid: %.2f%n", grand);
    }

    private String readMonth() {
        String m = InputHelper.readString("Billing month (yyyy-MM, e.g. 2026-11): ");
        if (!m.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            System.out.println("Invalid month format.");
            return null;
        }
        return m;
    }
}