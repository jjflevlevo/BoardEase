package boardinghouse.service;

import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.MaintenanceDAO;
import boardinghouse.model.MaintenanceRequest;
import boardinghouse.model.RequestStatus;
import boardinghouse.model.Tenant;
import java.util.Arrays;

public class MaintenanceService {
    public static final String[] CATEGORIES = {"Plumbing", "Electrical", "Furniture", "Cleaning", "Internet", "Others"};
    public static final int MIN_DESCRIPTION = 10;
    public static final int MAX_DESCRIPTION = 500;
    public static final int MAX_NOTE = 300;

    private final MaintenanceDAO dao = new MaintenanceDAO();
    private final ActivityDAO activity = new ActivityDAO();

    // tenant sends a new request; returns null when sent, otherwise what is wrong
    public String submit(Tenant tenant, String category, String description) {
        if (tenant == null) return "Tenant not found.";
        if (category == null || !Arrays.asList(CATEGORIES).contains(category)) return "Choose a category.";
        if (description == null || description.length() < MIN_DESCRIPTION) {
            return "Describe the problem in at least " + MIN_DESCRIPTION + " characters.";
        }
        if (description.length() > MAX_DESCRIPTION) {
            return "The description is too long (max " + MAX_DESCRIPTION + " characters).";
        }
        if (!dao.insert(tenant.getTenantId(), tenant.getRoomId(), category, description)) {
            return "Could not send the request. Try again.";
        }
        activity.log("PENDING", "Maintenance request from " + tenant.getName() + ": " + category);
        return null;
    }

    public String cancel(int tenantId, int requestId) {
        return dao.deleteOpen(requestId, tenantId) ? null : "Only requests that are still open can be cancelled.";
    }

    // owner changes the status (OPEN, IN_PROGRESS, DONE) and can leave a note for the tenant
    public String updateStatus(int requestId, String status, String note) {
        RequestStatus target;
        try {
            target = RequestStatus.from(status);
        } catch (IllegalArgumentException e) {
            return "Unknown status.";
        }
        if (note != null && note.length() > MAX_NOTE) return "The note is too long (max " + MAX_NOTE + " characters).";

        MaintenanceRequest r = dao.findById(requestId);
        if (r == null) return "Request not found.";
        String cleanNote = note == null || note.isBlank() ? null : note;
        if (!dao.updateStatus(requestId, target.name(), cleanNote)) return "Could not update the request.";

        switch (target) {
            case DONE -> activity.log("PAID", "Maintenance done for " + r.getTenantName() + ": " + r.getCategory());
            case IN_PROGRESS -> activity.log("SYSTEM", "Maintenance started for " + r.getTenantName() + ": " + r.getCategory());
            default -> { }
        }
        return null;
    }
}