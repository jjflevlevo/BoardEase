package boardinghouse.ui;

import boardinghouse.model.Account;
import boardinghouse.model.Tenant;
import boardinghouse.service.AuthService;
import boardinghouse.util.InputHelper;
import boardinghouse.util.Validator;

public class LoginMenu implements Menu {
    private final AuthService auth = new AuthService();

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== Boarding House System ===");
            System.out.println("1. Login\n2. Sign up\n3. Exit");
            int choice = InputHelper.readInt("Choose: ");
            if (choice == 1) login();
            else if (choice == 2) signUp();
            else if (choice == 3) { System.out.println("Goodbye!"); return; }
            else System.out.println("Invalid choice.");
        }
    }

    private void login() {
        String email = InputHelper.readString("Email: ");
        String password = InputHelper.readString("Password: ");
        Account acc = auth.login(email, password);
        if (acc == null) {
            System.out.println("Invalid email or password.");
            return;
        }
        Menu menu = acc.getRole().equals("OWNER") ? new OwnerMenu(acc) : new TenantMenu(acc);
        menu.show();
    }

    private void signUp() {
        System.out.println("\n--- Step 1 of 2: Account ---");
        String email = readEmail();
        String phone = readPhone("Phone no. (09XXXXXXXXX): ");
        String password = readPassword();

        System.out.println("\n--- Step 2 of 2: Personal info ---");
        String name = readRequired("Full name: ");
        String address = readRequired("Address: ");
        int age = readAge();
        String occupation = readRequired("Occupation: ");
        String emergency = readPhone("Emergency no. (09XXXXXXXXX): ");

        Tenant t = new Tenant(name, address, age, occupation, emergency);
        System.out.println(auth.signUp(email, phone, password, t)
                ? "Account created! You can now log in."
                : "Sign up failed. That email is already used.");
    }

    private String readEmail() {
        while (true) {
            String v = InputHelper.readString("Email: ");
            if (Validator.isValidEmail(v)) return v;
            System.out.println("Enter a valid email address.");
        }
    }

    private String readPhone(String prompt) {
        while (true) {
            String v = InputHelper.readString(prompt);
            if (Validator.isValidPhone(v)) return v;
            System.out.println("Enter an 11-digit mobile number starting with 09.");
        }
    }

    private String readPassword() {
        while (true) {
            String v = InputHelper.readString("Password (6+ characters): ");
            if (Validator.isValidPassword(v)) return v;
            System.out.println("Password must be at least 6 characters.");
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            String v = InputHelper.readString(prompt);
            if (Validator.isNotBlank(v)) return v;
            System.out.println("This field is required.");
        }
    }

    private int readAge() {
        while (true) {
            int v = InputHelper.readInt("Age: ");
            if (Validator.isValidAge(v)) return v;
            System.out.println("Age must be between 16 and 100.");
        }
    }
}