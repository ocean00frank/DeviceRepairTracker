package com.devicerepair.api;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.exception.InvalidRepairStatusException;
import com.devicerepair.model.*;
import com.devicerepair.service.RepairManager;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiController {
    public void registerRoutes(Javalin app, RepairManager manager) {
        app.get("/api/health", ctx -> {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("database", "connected");
            response.put("databaseName", DatabaseConfig.getDbName());
            ctx.json(response);
        });

        app.get("/api/dashboard", ctx -> {
            Map<String, Object> stats = manager.getDashboardStats();
            ctx.json(stats);
        });

        app.post("/api/customers", ctx -> {
            try {
                CustomerRequest request = ctx.bodyAsClass(CustomerRequest.class);
                Customer customer = manager.registerCustomer(request.fullName(), request.phoneNumber());
                ctx.status(201).json(customer);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid customer input", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to save customer"));
            }
        });

        app.get("/api/customers", ctx -> {
            try {
                List<Customer> customers = manager.getAllCustomers();
                ctx.json(customers);
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to list customers"));
            }
        });

        app.get("/api/devices", ctx -> {
            try {
                List<Device> devices = manager.getAllDevices();
                ctx.json(devices);
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to list devices"));
            }
        });

        app.post("/api/devices", ctx -> {
            try {
                DeviceRequest request = ctx.bodyAsClass(DeviceRequest.class);
                Device device = manager.registerDevice(request.customerId(), request.deviceType(), request.brand(), request.model());
                ctx.status(201).json(device);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid device input", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to save device"));
            }
        });

        app.post("/api/repair-jobs", ctx -> {
            try {
                JobRequest request = ctx.bodyAsClass(JobRequest.class);
                RepairJob job = manager.createRepairJob(request.customerId(), request.deviceId(), request.reportedFault(), request.repairNotes());
                ctx.status(201).json(job);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid repair job input", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to create repair job"));
            }
        });

        app.get("/api/repair-jobs", ctx -> {
            try {
                ctx.json(manager.getAllRepairJobs());
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to list repair jobs"));
            }
        });

        app.get("/api/repair-jobs/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RepairJob job = manager.getRepairJobById(id);
                if (job == null) {
                    ctx.status(404).json(errorResponse("Not found", "Repair job not found"));
                    return;
                }
                job.setStatus(job.getStatus());
                List<RepairRecord> records = manager.getRepairHistory(id);
                Map<String, Object> response = new HashMap<>();
                response.put("job", job);
                response.put("records", records);
                response.put("allowedNextStatuses", manager.getAllowedNextStatuses(job.getStatus()));
                ctx.json(response);
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to fetch repair job"));
            }
        });

        app.get("/api/repair-jobs/search", ctx -> {
            String query = ctx.queryParam("q");
            try {
                ctx.json(manager.searchRepairJobs(query));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to search repair jobs"));
            }
        });

        app.put("/api/repair-jobs/{id}/status", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                StatusRequest request = ctx.bodyAsClass(StatusRequest.class);
                RepairStatus nextStatus = RepairStatus.fromString(request.status());
                RepairJob job = manager.updateRepairStatus(id, nextStatus, request.notes());
                ctx.json(job);
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid status input", e.getMessage()));
            } catch (InvalidRepairStatusException e) {
                ctx.status(409).json(errorResponse("Invalid status transition", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to update status"));
            }
        });

        app.get("/api/repair-jobs/{id}/records", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RepairJob job = manager.getRepairJobById(id);
                if (job == null) {
                    ctx.status(404).json(errorResponse("Not found", "Repair job not found"));
                    return;
                }
                ctx.json(manager.getRepairHistory(id));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to fetch status history"));
            }
        });

        app.get("/api/trackable-demo", ctx -> {
            List<Trackable> trackables = new java.util.ArrayList<>();
            trackables.add(new RepairJob(1, 1, 1, "Screen cracked", "Needs screen replacement"));
            trackables.add(new RepairRecord(1, 1, RepairStatus.RECEIVED, RepairStatus.IN_PROGRESS, "Started diagnosis"));
            ctx.json(trackables.stream().map(Trackable::trackStatus).toList());
        });
    }

    private Map<String, String> errorResponse(String type, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", type);
        response.put("message", message);
        return response;
    }

    public record CustomerRequest(String fullName, String phoneNumber) {
    }

    public record DeviceRequest(int customerId, String deviceType, String brand, String model) {
    }

    public record JobRequest(int customerId, int deviceId, String reportedFault, String repairNotes) {
    }

    public record StatusRequest(String status, String notes) {
    }
}
