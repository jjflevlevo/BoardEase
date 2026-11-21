package boardinghouse.gui;

import boardinghouse.exception.ValidationException;
import boardinghouse.model.Account;
import boardinghouse.model.Tenant;
import boardinghouse.service.ProfileService;
import javax.swing.*;
import java.awt.*;

// the Edit Profile form: tenants can change their contact details, the owner can change everything
public class ProfileDialog {
    private ProfileDialog() { }

    // returns true when the changes were saved
    public static boolean edit(Component parent, Tenant tenant, Account account, boolean ownerMode) {
        JTextField name = Theme.field("Full name");
        JTextField email = Theme.field("name@example.com");
        JTextField phone = Theme.field("09XXXXXXXXX");
        JTextField emergency = Theme.field("09XXXXXXXXX");
        JTextField address = Theme.field("Home address");
        JTextField occupation = Theme.field("Occupation");
        JTextField age = Theme.field("Age");

        name.setText(tenant.getName());
        email.setText(account.getEmail());
        phone.setText(account.getPhone());
        emergency.setText(tenant.getEmergencyNo());
        address.setText(tenant.getAddress());
        occupation.setText(tenant.getOccupation());
        age.setText(String.valueOf(tenant.getAge()));

        if (!ownerMode) {
            name.setEditable(false);
            email.setEditable(false);
        }

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 0));
        grid.setOpaque(false);
        grid.add(Theme.labeled("Full name", name));
        grid.add(Theme.labeled("Email", email));
        grid.add(Theme.labeled("Phone no.", phone));
        grid.add(Theme.labeled("Emergency contact no.", emergency));
        grid.add(Theme.labeled("Address", address));
        grid.add(Theme.labeled("Occupation", occupation));
        grid.add(Theme.labeled("Age", age));
        grid.add(new JLabel());

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        if (!ownerMode) content.add(Theme.muted("To change your name or email, ask the owner."), BorderLayout.SOUTH);
        content.setPreferredSize(new Dimension(600, content.getPreferredSize().height));

        ProfileService profile = new ProfileService();
        String title = ownerMode ? "Edit Tenant: " + tenant.getName() : "Edit My Profile";
        while (true) {
            int ok = JOptionPane.showConfirmDialog(parent, content, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return false;

            try {
                profile.updateTenant(tenant, name.getText().trim(), email.getText().trim(),
                        phone.getText().trim(), address.getText().trim(), age.getText().trim(),
                        occupation.getText().trim(), emergency.getText().trim());
                JOptionPane.showMessageDialog(parent, "Profile saved.");
                return true;
            } catch (ValidationException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "Check the details", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}