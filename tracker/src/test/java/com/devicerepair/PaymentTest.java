package com.devicerepair;

import com.devicerepair.model.Payment;
import com.devicerepair.model.RepairJob;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void acceptsValidRepairCostAndPayment() {
        RepairJob job = new RepairJob();
        job.setRepairCost(new BigDecimal("125.50"));

        Payment payment = new Payment(1, 10, new BigDecimal("25.50"), "Cash", LocalDateTime.now());

        assertEquals(new BigDecimal("125.50"), job.getRepairCost());
        assertEquals(new BigDecimal("25.50"), payment.getAmount());
        assertEquals("Cash", payment.getPaymentMethod());
        assertEquals("PAID", payment.getPaymentStatus());
        assertTrue(payment.trackStatus().contains("PAID"));
    }

    @Test
    void rejectsInvalidPaymentAmountsAndMethods() {
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(1, 10, BigDecimal.ZERO, "Cash", LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(1, 10, new BigDecimal("1.001"), "Cash", LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(1, 10, BigDecimal.ONE, " ", LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
            () -> new Payment(1, 10, BigDecimal.ONE, "Cash", "UNKNOWN", LocalDateTime.now()));
    }

    @Test
    void rejectsRepairCostsThatAreZeroOrHaveTooManyDecimalPlaces() {
        RepairJob job = new RepairJob();

        assertThrows(IllegalArgumentException.class, () -> job.setRepairCost(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> job.setRepairCost(new BigDecimal("12.345")));
    }
}