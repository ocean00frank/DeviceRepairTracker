package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepairHistoryRepository {
    public List<Map<String, Object>> findByRepairJobId(int repairJobId) throws SQLException {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT id, repair_job_id, previous_status, new_status, notes, recorded_at FROM repair_records WHERE repair_job_id = ? ORDER BY recorded_at, id";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repairJobId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", rs.getInt("id"));
                    entry.put("repairJobId", rs.getInt("repair_job_id"));
                    entry.put("previousStatus", rs.getString("previous_status"));
                    entry.put("newStatus", rs.getString("new_status"));
                    entry.put("notes", rs.getString("notes"));
                    entry.put("recordedAt", rs.getTimestamp("recorded_at").toLocalDateTime());
                    history.add(entry);
                }
            }
        }
        return history;
    }
}