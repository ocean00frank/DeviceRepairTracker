package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.model.RepairRecord;
import com.devicerepair.model.RepairStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepairRecordRepository {
    public List<RepairRecord> findByRepairJobId(int repairJobId) throws SQLException {
        List<RepairRecord> records = new ArrayList<>();
        String sql = "SELECT id, repair_job_id, previous_status, new_status, notes, recorded_at FROM repair_records WHERE repair_job_id = ? ORDER BY recorded_at ASC";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    RepairRecord record = new RepairRecord();
                    record.setId(rs.getInt("id"));
                    record.setRepairJobId(rs.getInt("repair_job_id"));
                    String previousStatus = rs.getString("previous_status");
                    String newStatus = rs.getString("new_status");
                    record.setPreviousStatus(previousStatus == null ? null : RepairStatus.fromString(previousStatus));
                    record.setNewStatus(RepairStatus.fromString(newStatus));
                    record.setNotes(rs.getString("notes"));
                    record.setRecordedAt(rs.getTimestamp("recorded_at").toLocalDateTime());
                    records.add(record);
                }
            }
        }
        return records;
    }
}
