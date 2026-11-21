package boardinghouse.model;

import java.time.LocalDate;

// anything a tenant can be charged for
public interface Chargeable {
    double computeAmount();

    String getType();

    double getLateFee(LocalDate today);
}