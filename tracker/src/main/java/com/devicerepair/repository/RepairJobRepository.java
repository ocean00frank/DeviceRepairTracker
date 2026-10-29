package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.model.RepairJob;
import com.devicerepair.model.RepairStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RepairJobRepository {
    public RepairJob save(RepairJob repairJob) throws SQLException {
        String sql = "INSERT INTO repair_jobs (customer_id, device_id, reported_fault, repair_notes, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, repairJob.getCustomerId());
            statement.setInt(2, repairJob.getDeviceId());
            statement.setString(3, repairJob.getReportedFault());
            statement.setString(4, repairJob.getRepairNotes());
            statement.setString(5, repairJob.getStatus().name());
            statement.setTimestamp(6, Timestamp.valueOf(repairJob.getCreatedAt()));
            statement.setTimestamp(7, Timestamp.valueOf(repairJob.getUpdatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    repairJob.setId(keys.getInt(1));
                }
            }
        }
        return repairJob;
    }

    public RepairJob findById(int id) throws SQLException {
        String sql = "SELECT id, customer_id, device_id, reported_fault, repair_notes, status, created_at, updated_at FROM repair_jobs WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    RepairJob job = new RepairJob();
                    job.setId(rs.getInt("id"));
                    job.setCustomerId(rs.getInt("customer_id"));
                    job.setDeviceId(rs.getInt("device_id"));
                    job.setReportedFault(rs.getString("reported_fault"));
                    job.setRepairNotes(rs.getString("repair_notes"));
                    job.setStatus(RepairStatus.fromString(rs.getString("status")));
                    job.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    job.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return job;
                }
            }
        }
        return null;
    }

    public List<RepairJob> findAll() throws SQLException {
        List<RepairJob> jobs = new ArrayList<>();
        String sql = "SELECT id, customer_id, device_id, reported_fault, repair_notes, status, created_at, updated_at FROM repair_jobs ORDER BY id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                RepairJob job = new RepairJob();
                job.setId(rs.getInt("id"));
                job.setCustomerId(rs.getInt("customer_id"));
                job.setDeviceId(rs.getInt("device_id"));
                job.setReportedFault(rs.getString("reported_fault"));
                job.setRepairNotes(rs.getString("repair_notes"));
                job.setStatus(RepairStatus.fromString(rs.getString("status")));
                job.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                job.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                jobs.add(job);
            }
        }
        return jobs;
    }

    public List<RepairJob> search(String query) throws SQLException {
        List<RepairJob> results = new ArrayList<>();
        String sql = "SELECT r.id, r.customer_id, r.device_id, r.reported_fault, r.repair_notes, r.status, r.created_at, r.updated_at " +
                "FROM repair_jobs r " +
                "LEFT JOIN customers c ON c.id = r.customer_id " +
                "LEFT JOIN devices d ON d.id = r.device_id " +
                "WHERE CAST(r.id AS TEXT) ILIKE ? OR c.full_name ILIKE ? OR c.phone_number ILIKE ? OR d.brand ILIKE ? OR d.model ILIKE ? OR r.reported_fault ILIKE ? " +
                "ORDER BY r.id";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String likeValue = "%" + query + "%";
            for (int i = 1; i <= 6; i++) {
                statement.setString(i, likeValue);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    RepairJob job = new RepairJob();
                    job.setId(rs.getInt("id"));
                    job.setCustomerId(rs.getInt("customer_id"));
                    job.setDeviceId(rs.getInt("device_id"));
                    job.setReportedFault(rs.getString("reported_fault"));
                    job.setRepairNotes(rs.getString("repair_notes"));
                    job.setStatus(RepairStatus.fromString(rs.getString("status")));
                    job.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    job.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    results.add(job);
                }
            }
        }
        return results;
    }

    public void updateStatus(int jobId, RepairStatus status, String notes) throws SQLException {
        String sql = "UPDATE repair_jobs SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, jobId);
            statement.executeUpdate();
        }

        String recordSql = "INSERT INTO repair_records (repair_job_id, previous_status, new_status, notes, recorded_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        if (notes == null) {
            notes = "";
        }

        RepairJob existingJob = findById(jobId);
        String previousStatus = existingJob == null ? null : existingJob.getStatus().name();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(recordSql)) {
            statement.setInt(1, jobId);
            statement.setString(2, previousStatus);
            statement.setString(3, status.name());
            statement.setString(4, notes);
            statement.executeUpdate();
        }
    }

    public List<RepairJob> findByStatus(RepairStatus status) throws SQLException {
        List<RepairJob> jobs = new ArrayList<>();
        String sql = "SELECT id, customer_id, device_id, reported_fault, repair_notes, status, created_at, updated_at FROM repair_jobs WHERE status = ? ORDER BY id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    RepairJob job = new RepairJob();
                    job.setId(rs.getInt("id"));
                    job.setCustomerId(rs.getInt("customer_id"));
                    job.setDeviceId(rs.getInt("device_id"));
                    job.setReportedFault(rs.getString("reported_fault"));
                    job.setRepairNotes(rs.getString("repair_notes"));
                    job.setStatus(RepairStatus.fromString(rs.getString("status")));
                    job.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    job.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    jobs.add(job);
                }
            }
        }
        return jobs;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM repair_jobs";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countByStatus(RepairStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM repair_jobs WHERE status = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
