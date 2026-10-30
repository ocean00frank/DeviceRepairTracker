package com.devicerepair.model;

import com.devicerepair.exception.InvalidRepairStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Holds repair details and controls valid progress from intake to completion.
public class RepairJob implements Trackable {
    private int id;
    private int customerId;
    private int deviceId;
    private String reportedFault;
    private String repairNotes;
    private BigDecimal repairCost;
    private RepairStatus status;
    private String repairOutcome = "PENDING";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Keep each status change so the job's progress can be reviewed later.
    private final List<String> statusHistory = new ArrayList<>();

    public RepairJob() {
        // New jobs immediately enter the repair queue.
        this.status = RepairStatus.IN_PROGRESS;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.statusHistory.add(this.status.name());
    }

    public RepairJob(int id, int customerId, int deviceId, String reportedFault, String repairNotes) {
        this();
        this.id = id;
        setCustomerId(customerId);
        setDeviceId(deviceId);
        setReportedFault(reportedFault);
        setRepairNotes(repairNotes);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        this.customerId = customerId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        if (deviceId <= 0) {
            throw new IllegalArgumentException("Device ID is required");
        }
        this.deviceId = deviceId;
    }

    public String getReportedFault() {
        return reportedFault;
    }

    public void setReportedFault(String reportedFault) {
        if (reportedFault == null || reportedFault.trim().isEmpty()) {
            throw new IllegalArgumentException("Reported fault is required");
        }
        this.reportedFault = reportedFault.trim();
    }

    public String getRepairNotes() {
        return repairNotes;
    }

    public void setRepairNotes(String repairNotes) {
        this.repairNotes = repairNotes == null ? "" : repairNotes.trim();
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        if (repairCost != null) {
            try {
                repairCost = repairCost.setScale(2, java.math.RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Repair cost can have at most two decimal places", e);
            }
            if (repairCost.signum() <= 0) {
                throw new IllegalArgumentException("Repair cost must be greater than zero");
            }
        }
        this.repairCost = repairCost;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public String getRepairOutcome() {
        return repairOutcome;
    }

    public void setRepairOutcome(String repairOutcome) {
        if (repairOutcome == null || !repairOutcome.trim().matches("(?i)PENDING|FIXED|NOT_FIXED")) {
            throw new IllegalArgumentException("Repair outcome must be PENDING, FIXED, or NOT_FIXED");
        }
        this.repairOutcome = repairOutcome.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public void setStatus(RepairStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        this.statusHistory.add(status.name());
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtString() {
        return createdAt == null ? null : createdAt.toString();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedAtString() {
        return updatedAt == null ? null : updatedAt.toString();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getStatusHistory() {
        // Return a read-only view so other classes cannot change the job's history directly.
        return Collections.unmodifiableList(statusHistory);
    }

    // Validate the next status before changing the job or recording its history.
    public void updateStatus(RepairStatus nextStatus) throws InvalidRepairStatusException {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (nextStatus == this.status) {
            return;
        }

        // Repairs must move forward one step at a time; completed jobs cannot move further.
        boolean validTransition = switch (this.status) {
            case IN_PROGRESS -> nextStatus == RepairStatus.READY_FOR_COLLECTION;
            case READY_FOR_COLLECTION -> nextStatus == RepairStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!validTransition) {
            throw new InvalidRepairStatusException(
                    "Invalid status transition from " + this.status + " to " + nextStatus + ". Allowed transitions are: IN_PROGRESS -> READY_FOR_COLLECTION, READY_FOR_COLLECTION -> COMPLETED."
            );
        }

        // Save the new status only after it passes the transition rules.
        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
        this.statusHistory.add(nextStatus.name());
    }

    @Override
    public String trackStatus() {
        return "RepairJob[" + id + "] status: " + status + ", history=" + String.join(" -> ", statusHistory);
    }
}
