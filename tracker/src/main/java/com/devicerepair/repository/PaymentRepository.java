package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.model.Payment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentRepository {
    public BigDecimal setRepairCost(int repairJobId, BigDecimal repairCost) throws SQLException {
        requireRepairJobId(repairJobId);
        BigDecimal normalizedCost = normalizePositiveAmount(repairCost, "Repair cost");
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockRepairJob(connection, repairJobId);
                BigDecimal reservedPayments = totalReservedPayments(connection, repairJobId);
                if (normalizedCost.compareTo(reservedPayments) < 0) {
                    throw new IllegalArgumentException("Repair cost cannot be less than the amount already paid or pending");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE repair_jobs SET repair_cost = ? WHERE id = ?")) {
                    statement.setBigDecimal(1, normalizedCost);
                    statement.setInt(2, repairJobId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
        return normalizedCost;
    }

    public Payment save(Payment payment) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockRepairJob(connection, payment.getRepairJobId());
                BigDecimal repairCost = findRepairCost(connection, payment.getRepairJobId());
                if (repairCost == null) {
                    throw new IllegalArgumentException("Set the repair cost before recording a payment");
                }
                BigDecimal reservedPayments = totalReservedPayments(connection, payment.getRepairJobId());
                if (!"FAILED".equals(payment.getPaymentStatus())
                        && reservedPayments.add(payment.getAmount()).compareTo(repairCost) > 0) {
                    throw new IllegalArgumentException("Payment exceeds the remaining repair cost");
                }

                String sql = "INSERT INTO payments (repair_job_id, amount, payment_method, payment_status, paid_at) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, payment.getRepairJobId());
                    statement.setBigDecimal(2, payment.getAmount());
                    statement.setString(3, payment.getPaymentMethod());
                    statement.setString(4, payment.getPaymentStatus());
                    statement.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            payment.setId(keys.getInt(1));
                        }
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
        return payment;
    }

    public Map<Integer, BigDecimal> findRepairCosts() throws SQLException {
        Map<Integer, BigDecimal> repairCosts = new HashMap<>();
        String sql = "SELECT id, repair_cost FROM repair_jobs WHERE repair_cost IS NOT NULL";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                repairCosts.put(rs.getInt("id"), rs.getBigDecimal("repair_cost"));
            }
        }
        return repairCosts;
    }

    public FinanceSnapshot getFinanceSnapshot(int repairJobId) throws SQLException {
        requireRepairJobId(repairJobId);
        try (Connection connection = DatabaseConfig.getConnection()) {
            BigDecimal repairCost = findRepairCost(connection, repairJobId);
            if (repairCost == null && !repairJobExists(connection, repairJobId)) {
                throw new IllegalArgumentException("Repair job not found");
            }
            BigDecimal totalPaid = totalPaid(connection, repairJobId);
            List<Payment> payments = findByRepairJobId(connection, repairJobId);
            BigDecimal balance = repairCost == null ? null : repairCost.subtract(totalPaid);
            return new FinanceSnapshot(repairCost, totalPaid, balance, payments);
        }
    }

    public List<Payment> findByRepairJobId(int repairJobId) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            return findByRepairJobId(connection, repairJobId);
        }
    }

    private List<Payment> findByRepairJobId(Connection connection, int repairJobId) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT id, repair_job_id, amount, payment_method, payment_status, paid_at FROM payments WHERE repair_job_id = ? ORDER BY paid_at, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    payments.add(new Payment(
                            rs.getInt("id"),
                            rs.getInt("repair_job_id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_method"),
                            rs.getString("payment_status"),
                            rs.getTimestamp("paid_at").toLocalDateTime()));
                }
            }
        }
        return payments;
    }

    private BigDecimal findRepairCost(Connection connection, int repairJobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT repair_cost FROM repair_jobs WHERE id = ?")) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("repair_cost") : null;
            }
        }
    }

    private BigDecimal totalPaid(Connection connection, int repairJobId) throws SQLException {
        return sumPayments(connection, repairJobId, "payment_status = 'PAID'");
    }

    private BigDecimal totalReservedPayments(Connection connection, int repairJobId) throws SQLException {
        return sumPayments(connection, repairJobId, "payment_status IN ('PAID', 'PENDING')");
    }

    private BigDecimal sumPayments(Connection connection, int repairJobId, String statusCondition) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM payments WHERE repair_job_id = ? AND " + statusCondition;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1).setScale(2, RoundingMode.UNNECESSARY);
            }
        }
    }

    private void lockRepairJob(Connection connection, int repairJobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM repair_jobs WHERE id = ? FOR UPDATE")) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Repair job not found");
                }
            }
        }
    }

    private boolean repairJobExists(Connection connection, int repairJobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM repair_jobs WHERE id = ?")) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount, String label) {
        if (amount == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(label + " can have at most two decimal places", e);
        }
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
        return normalized;
    }

    private void requireRepairJobId(int repairJobId) {
        if (repairJobId <= 0) {
            throw new IllegalArgumentException("Repair job ID is required");
        }
    }

    public record FinanceSnapshot(BigDecimal repairCost, BigDecimal totalPaid, BigDecimal balance, List<Payment> payments) {
    }
}