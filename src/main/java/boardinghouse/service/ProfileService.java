package boardinghouse.service;

import boardinghouse.dao.AccountDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.exception.ValidationException;
import boardinghouse.model.Account;
import boardinghouse.model.Tenant;
import boardinghouse.util.Validator;

public class ProfileService {
    private final AccountDAO accountDAO = new AccountDAO();
    private final TenantDAO tenantDAO = new TenantDAO();

    // saves a tenant's details; throws ValidationException when something is missing or wrong
    public void updateTenant(Tenant tenant, String name, String email, String phone, String address,
                             String ageText, String occupation, String emergency) throws ValidationException {
        if (!Validator.isNotBlank(name)) throw new ValidationException("Enter the full name.");
        if (!Validator.isValidEmail(email)) throw new ValidationException("Enter a valid email address.");
        Account existing = accountDAO.findByEmail(email);
        if (existing != null && existing.getAccountId() != tenant.getAccountId()) {
            throw new ValidationException("That email is already used by another account.");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new ValidationException("The phone no. must be 11 digits starting with 09.");
        }
        if (!Validator.isNotBlank(address)) throw new ValidationException("Enter the address.");

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            throw new ValidationException("Age must be a number.");
        }
        if (!Validator.isValidAge(age)) throw new ValidationException("Age must be between 16 and 100.");
        if (!Validator.isNotBlank(occupation)) throw new ValidationException("Enter the occupation.");
        if (!Validator.isValidPhone(emergency)) {
            throw new ValidationException("The emergency no. must be 11 digits starting with 09.");
        }

        if (!accountDAO.updateContact(tenant.getAccountId(), email, phone)) {
            throw new ValidationException("Could not save the email and phone. Try again.");
        }
        if (!tenantDAO.updateInfo(tenant.getTenantId(), name, address, age, occupation, emergency)) {
            throw new ValidationException("Could not save the profile. Try again.");
        }
    }
}