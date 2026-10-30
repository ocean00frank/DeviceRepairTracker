package com.devicerepair.api;

import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.exception.InvalidRepairStatusException;
import com.devicerepair.model.*;
import com.devicerepair.service.RepairManager;
import io.javalin.Javalin;

import java.math.BigDecimal;
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
                RepairJob job = manager.createRepairJob(request.customerId(), request.deviceId(), request.reportedFault(),
                    request.repairNotes(), request.repairCost(), request.paymentStatus());
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

        app.get("/api/repair-jobs/search", ctx -> {
            String query = ctx.queryParam("q");
            try {
                ctx.json(manager.searchRepairJobs(query));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to search repair jobs"));
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
                List<Map<String, Object>> records = manager.getRepairHistory(id);
                Map<String, Object> response = new HashMap<>();
                response.put("job", job);
                response.put("records", records);
                response.put("allowedNextStatuses", manager.getAllowedNextStatuses(job.getStatus()));
                response.put("finance", manager.getFinanceSnapshot(id));
                ctx.json(response);
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to fetch repair job"));
            }
        });

        app.put("/api/repair-jobs/{id}/cost", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                CostRequest request = ctx.bodyAsClass(CostRequest.class);
                ctx.json(manager.setRepairCost(id, request.repairCost()));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid repair cost", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to verify repair job"));
            }
        });

        app.put("/api/repair-jobs/{id}/outcome", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                OutcomeRequest request = ctx.bodyAsClass(OutcomeRequest.class);
                ctx.json(manager.updateRepairOutcome(id, request.repairOutcome()));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid repair outcome", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to save repair outcome"));
            }
        });

        app.put("/api/repair-jobs/{id}/payment-status", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                PaymentStatusRequest request = ctx.bodyAsClass(PaymentStatusRequest.class);
                ctx.json(manager.updatePaymentStatus(id, request.paymentStatus()));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid payment status", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to save payment status"));
            }
        });

        app.put("/api/repair-jobs/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                JobDetailsRequest request = ctx.bodyAsClass(JobDetailsRequest.class);
                ctx.json(manager.updateRepairDetails(id, request.reportedFault(), request.repairNotes()));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid repair details", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to update repair details"));
            }
        });

        app.get("/api/repair-jobs/{id}/payments", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                ctx.json(manager.getFinanceSnapshot(id));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(errorResponse("Not found", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to verify repair job"));
            }
        });

        app.post("/api/repair-jobs/{id}/payments", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                PaymentRequest request = ctx.bodyAsClass(PaymentRequest.class);
                String paymentStatus = request.paymentStatus() == null ? "PAID" : request.paymentStatus();
                ctx.status(201).json(manager.recordPayment(id, request.amount(), request.paymentMethod(), paymentStatus));
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorResponse("Invalid ID", "Repair job ID must be a number"));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(errorResponse("Invalid payment", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(errorResponse("Database error", "Unable to verify repair job"));
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
            trackables.add(new Payment(1, 1, new BigDecimal("25.00"), "Cash", "PAID", java.time.LocalDateTime.now()));
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

    public record JobRequest(int customerId, int deviceId, String reportedFault, String repairNotes,
                             BigDecimal repairCost, String paymentStatus) {
    }

    public record CostRequest(BigDecimal repairCost) {
    }

    public record OutcomeRequest(String repairOutcome) {
    }

    public record JobDetailsRequest(String reportedFault, String repairNotes) {
    }

    public record PaymentRequest(BigDecimal amount, String paymentMethod, String paymentStatus) {
    }

    public record PaymentStatusRequest(String paymentStatus) {
    }

    public record StatusRequest(String status, String notes) {
    }
}
