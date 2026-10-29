package com.devicerepair.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepairRecord implements Trackable {
    private int id;
    private int repairJobId;
    private RepairStatus previousStatus;
    private RepairStatus newStatus;
    private String notes;
    private LocalDateTime recordedAt;
    private final List<String> history = new ArrayList<>();

    public RepairRecord() {
        this.recordedAt = LocalDateTime.now();
    }

    public RepairRecord(int id, int repairJobId, RepairStatus previousStatus, RepairStatus newStatus, String notes) {
        this();
        this.id = id;
        this.repairJobId = repairJobId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.notes = notes == null ? "" : notes.trim();
        this.history.add(previousStatus == null ? "CREATED" : previousStatus.name());
        this.history.add(newStatus.name());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRepairJobId() {
        return repairJobId;
    }

    public void setRepairJobId(int repairJobId) {
        this.repairJobId = repairJobId;
    }

    public RepairStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(RepairStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public RepairStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(RepairStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public String getRecordedAtString() {
        return recordedAt == null ? null : recordedAt.toString();
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public String trackStatus() {
        String previous = previousStatus == null ? "created" : previousStatus.name();
        return "RepairRecord[" + id + "] changed from " + previous + " to " + newStatus + ", history=" + String.join(" -> ", history);
    }
}
