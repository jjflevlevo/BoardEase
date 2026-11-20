package boardinghouse.util;

public class Validator {
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    // Philippine mobile number, e.g. 09123456789
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^09\\d{9}$");
    }

    public static boolean isValidAge(int age) {
        return age >= 16 && age <= 100;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}