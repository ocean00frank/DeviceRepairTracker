package com.devicerepair.model;

public enum RepairStatus {
    RECEIVED,
    IN_PROGRESS,
    READY_FOR_COLLECTION,
    COMPLETED;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        for (RepairStatus status : RepairStatus.values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }

        return false;
    }

    public static RepairStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        for (RepairStatus status : RepairStatus.values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid status: " + value);
    }
}
