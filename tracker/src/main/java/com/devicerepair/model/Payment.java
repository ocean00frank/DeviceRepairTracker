package com.devicerepair.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// Records one payment received for a repair job.
public class Payment implements Trackable {
    private int id;
    private int repairJobId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus = "PAID";
    private LocalDateTime paidAt;

    public Payment() {
    }

    public Payment(int id, int repairJobId, BigDecimal amount, String paymentMethod, LocalDateTime paidAt) {
        this(id, repairJobId, amount, paymentMethod, "PAID", paidAt);
    }

    public Payment(int id, int repairJobId, BigDecimal amount, String paymentMethod, String paymentStatus, LocalDateTime paidAt) {
        setId(id);
        setRepairJobId(repairJobId);
        setAmount(amount);
        setPaymentMethod(paymentMethod);
        setPaymentStatus(paymentStatus);
        setPaidAt(paidAt);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Payment ID cannot be negative");
        }
        this.id = id;
    }

    public int getRepairJobId() {
        return repairJobId;
    }

    public void setRepairJobId(int repairJobId) {
        if (repairJobId <= 0) {
            throw new IllegalArgumentException("Repair job ID is required");
        }
        this.repairJobId = repairJobId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        try {
            this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Payment amount can have at most two decimal places", e);
        }
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        this.paymentMethod = paymentMethod.trim();
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        if (paymentStatus == null || !paymentStatus.trim().matches("(?i)PENDING|PAID|FAILED")) {
            throw new IllegalArgumentException("Payment status must be PENDING, PAID, or FAILED");
        }
        this.paymentStatus = paymentStatus.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        if (paidAt == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        this.paidAt = paidAt;
    }

    @Override
    public String trackStatus() {
        return "Payment[" + id + "] " + amount + " via " + paymentMethod + " is " + paymentStatus;
    }
}