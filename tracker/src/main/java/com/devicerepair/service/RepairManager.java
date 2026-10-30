package com.devicerepair.service;

import com.devicerepair.exception.InvalidRepairStatusException;
import com.devicerepair.model.*;
import com.devicerepair.repository.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Coordinates validation and repository calls for repair-shop operations.
public class RepairManager {
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final RepairJobRepository repairJobRepository = new RepairJobRepository();
    private final RepairHistoryRepository repairHistoryRepository = new RepairHistoryRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public Customer registerCustomer(String fullName, String phoneNumber) throws SQLException {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setPhoneNumber(phoneNumber);
        return customerRepository.save(customer);
    }

    public Device registerDevice(int customerId, String deviceType, String brand, String model) throws SQLException {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }

        Device device = new Device();
        device.setCustomerId(customerId);
        device.setDeviceType(deviceType);
        device.setBrand(brand);
        device.setModel(model);
        return deviceRepository.save(device);
    }

    public RepairJob createRepairJob(int customerId, int deviceId, String reportedFault, String repairNotes) throws SQLException {
        Customer customer = customerRepository.findById(customerId);
        Device device = deviceRepository.findById(deviceId);

        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        if (device == null || device.getCustomerId() != customerId) {
            throw new IllegalArgumentException("Device not found for this customer");
        }

        RepairJob job = new RepairJob();
        job.setCustomerId(customerId);
        job.setDeviceId(deviceId);
        job.setReportedFault(reportedFault);
        job.setRepairNotes(repairNotes);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(job.getCreatedAt());

        return repairJobRepository.save(job);
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerRepository.findAll();
    }

    public List<Device> getAllDevices() throws SQLException {
        return deviceRepository.findAll();
    }

    public List<RepairJob> getAllRepairJobs() throws SQLException {
        return repairJobRepository.findAll();
    }

    public RepairJob getRepairJobById(int id) throws SQLException {
        return repairJobRepository.findById(id);
    }

    public List<RepairJob> searchRepairJobs(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return repairJobRepository.findAll();
        }
        return repairJobRepository.search(query.trim());
    }

    public RepairJob setRepairCost(int jobId, BigDecimal repairCost) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        job.setRepairCost(paymentRepository.setRepairCost(jobId, repairCost));
        return job;
    }

    public Payment recordPayment(int jobId, BigDecimal amount, String paymentMethod, String paymentStatus) throws SQLException {
        if (repairJobRepository.findById(jobId) == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        Payment payment = new Payment(0, jobId, amount, paymentMethod, paymentStatus, LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public PaymentRepository.FinanceSnapshot getFinanceSnapshot(int jobId) throws SQLException {
        if (repairJobRepository.findById(jobId) == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        return paymentRepository.getFinanceSnapshot(jobId);
    }

    public RepairJob updateRepairOutcome(int jobId, String repairOutcome) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }

        job.setRepairOutcome(repairOutcome);
        repairJobRepository.updateRepairOutcome(jobId, job.getRepairOutcome());
        return repairJobRepository.findById(jobId);
    }

    public RepairJob updateRepairDetails(int jobId, String reportedFault, String repairNotes) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }

        job.setReportedFault(reportedFault);
        job.setRepairNotes(repairNotes);
        repairJobRepository.updateRepairDetails(jobId, job.getReportedFault(), job.getRepairNotes());
        return repairJobRepository.findById(jobId);
    }

    public RepairJob updateRepairStatus(int jobId, RepairStatus newStatus, String notes) throws SQLException, InvalidRepairStatusException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }

        RepairStatus previous = job.getStatus();
        if (newStatus == previous) {
            return job;
        }

        boolean validTransition = switch (previous) {
            case IN_PROGRESS -> newStatus == RepairStatus.READY_FOR_COLLECTION;
            case READY_FOR_COLLECTION -> newStatus == RepairStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!validTransition) {
            throw new InvalidRepairStatusException(
                    "Invalid status transition from " + previous + " to " + newStatus + ". Allowed transitions are: IN_PROGRESS -> READY_FOR_COLLECTION, READY_FOR_COLLECTION -> COMPLETED."
            );
        }

        job.updateStatus(newStatus);
        repairJobRepository.updateStatus(jobId, previous, newStatus, notes);
        return job;
    }

    public List<Map<String, Object>> getRepairHistory(int repairJobId) throws SQLException {
        return repairHistoryRepository.findByRepairJobId(repairJobId);
    }

    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", repairJobRepository.countAll());
        stats.put("jobsInProgress", repairJobRepository.countByStatus(RepairStatus.IN_PROGRESS));
        stats.put("jobsReadyForCollection", repairJobRepository.countByStatus(RepairStatus.READY_FOR_COLLECTION));
        stats.put("completedJobs", repairJobRepository.countByStatus(RepairStatus.COMPLETED));
        return stats;
    }

    public List<String> getAllowedNextStatuses(RepairStatus status) {
        List<String> statuses = new ArrayList<>();
        if (status == RepairStatus.IN_PROGRESS) {
            statuses.add(RepairStatus.READY_FOR_COLLECTION.name());
        } else if (status == RepairStatus.READY_FOR_COLLECTION) {
            statuses.add(RepairStatus.COMPLETED.name());
        }
        return statuses;
    }

    public List<Trackable> getTrackableJobs() throws SQLException {
        List<Trackable> list = new ArrayList<>();
        for (RepairJob job : repairJobRepository.findAll()) {
            list.add(job);
        }
        return list;
    }

}
