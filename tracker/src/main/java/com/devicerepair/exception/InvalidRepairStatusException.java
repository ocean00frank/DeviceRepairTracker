package com.devicerepair.exception;

// A checked exception used when a repair job skips an allowed status step.
// Reports an invalid repair status change.
public class InvalidRepairStatusException extends Exception {
    public InvalidRepairStatusException(String message) {
        super(message);
    }
}
