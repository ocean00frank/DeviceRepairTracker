package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.model.Device;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeviceRepository {
    public Device save(Device device) throws SQLException {
        String sql = "INSERT INTO devices (customer_id, device_type, brand, model) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, device.getCustomerId());
            statement.setString(2, device.getDeviceType());
            statement.setString(3, device.getBrand());
            statement.setString(4, device.getModel());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    device.setId(keys.getInt(1));
                }
            }
        }
        return device;
    }

    public Device findById(int deviceId) throws SQLException {
        String sql = "SELECT id, customer_id, device_type, brand, model FROM devices WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deviceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Device(
                            rs.getInt("id"),
                            rs.getInt("customer_id"),
                            rs.getString("device_type"),
                            rs.getString("brand"),
                            rs.getString("model")
                    );
                }
            }
        }
        return null;
    }

    public List<Device> findAll() throws SQLException {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT id, customer_id, device_type, brand, model FROM devices ORDER BY id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                devices.add(new Device(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getString("device_type"),
                        rs.getString("brand"),
                        rs.getString("model")
                ));
            }
        }
        return devices;
    }

    public List<Device> findByCustomerId(int customerId) throws SQLException {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT id, customer_id, device_type, brand, model FROM devices WHERE customer_id = ? ORDER BY id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    devices.add(new Device(
                            rs.getInt("id"),
                            rs.getInt("customer_id"),
                            rs.getString("device_type"),
                            rs.getString("brand"),
                            rs.getString("model")
                    ));
                }
            }
        }
        return devices;
    }
}
