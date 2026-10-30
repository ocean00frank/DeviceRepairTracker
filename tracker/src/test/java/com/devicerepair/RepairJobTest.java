package com.devicerepair;

import com.devicerepair.exception.InvalidRepairStatusException;
import com.devicerepair.model.RepairJob;
import com.devicerepair.model.RepairStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepairJobTest {

    @Test
    void allowsValidStatusProgression() throws InvalidRepairStatusException {
        RepairJob job = new RepairJob(1, 10, 20, "Screen cracked", "Customer reported a cracked screen");

        assertEquals(RepairStatus.IN_PROGRESS, job.getStatus());

        job.updateStatus(RepairStatus.READY_FOR_COLLECTION);
        assertEquals(RepairStatus.READY_FOR_COLLECTION, job.getStatus());

        job.updateStatus(RepairStatus.COMPLETED);
        assertEquals(RepairStatus.COMPLETED, job.getStatus());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        RepairJob job = new RepairJob(1, 10, 20, "Battery issue", "Battery replacement needed");

        InvalidRepairStatusException thrown = assertThrows(
                InvalidRepairStatusException.class,
                () -> job.updateStatus(RepairStatus.COMPLETED)
        );

        assertTrue(thrown.getMessage().contains("IN_PROGRESS"));
    }

    @Test
    void tracksWhetherTheRepairWasFixed() {
        RepairJob job = new RepairJob();

        assertEquals("PENDING", job.getRepairOutcome());
        job.setRepairOutcome("fixed");

        assertEquals("FIXED", job.getRepairOutcome());
        assertThrows(IllegalArgumentException.class, () -> job.setRepairOutcome("MAYBE"));
    }

    @Test
    void trackStatusProvidesRuntimeStatusSummary() {
        RepairJob job = new RepairJob(1, 10, 20, "Charging port", "Need port replacement");
        var summary = job.trackStatus();

        assertTrue(summary.contains("IN_PROGRESS"));
    }

    @Test
    void jsonMapperSerializesLocalDateTimeFields() throws JsonProcessingException {
        RepairJob job = new RepairJob(1, 10, 20, "Screen cracked", "Customer reported a cracked screen");

        String json = JavalinJackson.defaultMapper().writeValueAsString(job);

        assertTrue(json.contains("createdAt"));
        assertTrue(json.contains("updatedAt"));
    }
}
