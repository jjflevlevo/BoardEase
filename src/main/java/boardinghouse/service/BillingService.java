package boardinghouse.service;

import boardinghouse.dao.*;
import boardinghouse.model.*;
import java.time.LocalDate;
import java.util.List;

public class BillingService {
    private final RoomDAO roomDAO = new RoomDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final BillDAO billDAO = new BillDAO();
    private final MeterReadingDAO meterDAO = new MeterReadingDAO();
    private final ActivityDAO activity = new ActivityDAO();

    // Overload 1: generate bills for all rooms
    public String generateBills(String month, LocalDate dueDate, double waterRate, double electricRate) {
        int[] total = new int[3];
        for (Room room : roomDAO.findAll()) {
            int[] r = generateForRoom(room, month, dueDate, waterRate, electricRate);
            for (int i = 0; i < 3; i++) total[i] += r[i];
        }
        if (total[0] > 0) activity.log("BILLS", total[0] + " bill(s) generated for " + month);
        return summary(total);
    }

    // Overload 2: generate bills for one room
    public String generateBills(Room room, String month, LocalDate dueDate, double waterRate, double electricRate) {
        int[] r = generateForRoom(room, month, dueDate, waterRate, electricRate);
        if (r[0] > 0) activity.log("BILLS", r[0] + " bill(s) generated for room " + room.getRoomNo() + ", " + month);
        return summary(r);
    }

    // result: [created, already existed, utility bills skipped]
    private int[] generateForRoom(Room room, String month, LocalDate dueDate, double waterRate, double electricRate) {
        int[] r = new int[3];
        List<Tenant> tenants = tenantDAO.findByRoom(room.getRoomId());
        int occupants = tenants.size();
        if (occupants == 0) return r;

        Double waterUsage = usage(room.getRoomId(), "WATER", month);
        Double electricUsage = usage(room.getRoomId(), "ELECTRIC", month);

        for (Tenant t : tenants) {
            int id = t.getTenantId();
            save(r, new RentBill(id, month, dueDate, room.getMonthlyRent()));

            if (waterUsage == null) r[2]++;
            else save(r, new WaterBill(id, month, dueDate, waterUsage, waterRate, occupants));

            if (electricUsage == null) r[2]++;
            else save(r, new ElectricBill(id, month, dueDate, electricUsage, electricRate, occupants));
        }
        return r;
    }

    // Works with any Bill type (polymorphism)
    private void save(int[] r, Bill bill) {
        if (billDAO.exists(bill.getTenantId(), bill.getType(), bill.getBillingMonth())) r[1]++;
        else if (billDAO.insert(bill)) r[0]++;
    }

    // usage = this month's reading minus the previous reading
    private Double usage(int roomId, String type, String month) {
        Double current = meterDAO.find(roomId, type, month);
        Double previous = meterDAO.findPrevious(roomId, type, month);
        if (current == null || previous == null || current < previous) return null;
        return current - previous;
    }

    private String summary(int[] r) {
        StringBuilder sb = new StringBuilder("\n--- Billing Result ---\n");
        sb.append("New bills created: ").append(r[0]).append("\n");
        sb.append("Skipped (already existed): ").append(r[1]);
        if (r[1] > 0) sb.append("  <- these bills were generated before");
        if (r[2] > 0) {
            sb.append("\nUtility bills skipped: ").append(r[2])
              .append("  <- missing meter readings for this month or an earlier month");
        }
        return sb.toString();
    }
}