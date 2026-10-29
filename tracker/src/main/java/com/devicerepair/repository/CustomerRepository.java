package com.devicerepair.repository;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {
    public Customer save(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (full_name, phone_number) VALUES (?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhoneNumber());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getInt(1));
                }
            }
        }
        return customer;
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT id, full_name, phone_number FROM customers ORDER BY id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                customers.add(new Customer(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("phone_number")
                ));
            }
        }
        return customers;
    }

    public Customer findById(int id) throws SQLException {
        String sql = "SELECT id, full_name, phone_number FROM customers WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Customer(rs.getInt("id"), rs.getString("full_name"), rs.getString("phone_number"));
                }
            }
        }
        return null;
    }
}
