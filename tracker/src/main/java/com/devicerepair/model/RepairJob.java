package com.devicerepair.model;

import com.devicerepair.exception.InvalidRepairStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepairJob implements Trackable {
    private int id;
    private int customerId;
    private int deviceId;
    private String reportedFault;
    private String repairNotes;
    private RepairStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<String> statusHistory = new ArrayList<>();

    public RepairJob() {
        this.status = RepairStatus.RECEIVED;
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

    public RepairStatus getStatus() {
        return status;
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
        return Collections.unmodifiableList(statusHistory);
    }

    public void updateStatus(RepairStatus nextStatus) throws InvalidRepairStatusException {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (nextStatus == this.status) {
            return;
        }

        boolean validTransition = switch (this.status) {
            case RECEIVED -> nextStatus == RepairStatus.IN_PROGRESS;
            case IN_PROGRESS -> nextStatus == RepairStatus.READY_FOR_COLLECTION;
            case READY_FOR_COLLECTION -> nextStatus == RepairStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!validTransition) {
            throw new InvalidRepairStatusException(
                    "Invalid status transition from " + this.status + " to " + nextStatus + ". Allowed transitions are: RECEIVED -> IN_PROGRESS, IN_PROGRESS -> READY_FOR_COLLECTION, READY_FOR_COLLECTION -> COMPLETED."
            );
        }

        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
        this.statusHistory.add(nextStatus.name());
    }

    @Override
    public String trackStatus() {
        return "RepairJob[" + id + "] status: " + status + ", history=" + String.join(" -> ", statusHistory);
    }
}
