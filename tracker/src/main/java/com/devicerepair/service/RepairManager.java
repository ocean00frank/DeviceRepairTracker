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

    // Creates a validated customer record.
    public Customer registerCustomer(String fullName, String phoneNumber) throws SQLException {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setPhoneNumber(phoneNumber);
        return customerRepository.save(customer);
    }

    // Registers a device under an existing customer.
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

    // Creates a repair job for the customer's device.
    public RepairJob createRepairJob(int customerId, int deviceId, String reportedFault, String repairNotes,
                                     BigDecimal repairCost, String paymentStatus) throws SQLException {
        if (repairCost == null) {
            throw new IllegalArgumentException("Repair price is required");
        }
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
        job.setRepairCost(repairCost);
        job.setPaymentStatus(paymentStatus == null ? "NOT_PAID" : paymentStatus);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(job.getCreatedAt());

        return repairJobRepository.save(job);
    }

    // Returns all registered customers.
    public List<Customer> getAllCustomers() throws SQLException {
        return customerRepository.findAll();
    }

    // Returns all registered devices.
    public List<Device> getAllDevices() throws SQLException {
        return deviceRepository.findAll();
    }

    // Updates a customer's contact details.
    public Customer updateCustomer(int id, String fullName, String phoneNumber) throws SQLException {
        Customer customer = customerRepository.findById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        customer.setFullName(fullName);
        customer.setPhoneNumber(phoneNumber);
        return customerRepository.update(customer);
    }

    // Deletes a customer when no related records exist.
    public void deleteCustomer(int id) throws SQLException {
        if (customerRepository.findById(id) == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        if (!deviceRepository.findByCustomerId(id).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete a customer with registered devices");
        }
        if (repairJobRepository.existsByCustomerId(id)) {
            throw new IllegalArgumentException("Cannot delete a customer with repair jobs");
        }
        customerRepository.deleteById(id);
    }

    // Updates device details and ownership.
    public Device updateDevice(int id, int customerId, String deviceType, String brand, String model) throws SQLException {
        if (customerRepository.findById(customerId) == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        Device device = deviceRepository.findById(id);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (device.getCustomerId() != customerId && repairJobRepository.existsByDeviceId(id)) {
            throw new IllegalArgumentException("Cannot change the customer for a device with repair jobs");
        }
        device.setCustomerId(customerId);
        device.setDeviceType(deviceType);
        device.setBrand(brand);
        device.setModel(model);
        return deviceRepository.update(device);
    }

    // Deletes a device without repair history.
    public void deleteDevice(int id) throws SQLException {
        if (deviceRepository.findById(id) == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (repairJobRepository.existsByDeviceId(id)) {
            throw new IllegalArgumentException("Cannot delete a device with repair jobs");
        }
        deviceRepository.deleteById(id);
    }

    // Returns all repair jobs.
    public List<RepairJob> getAllRepairJobs() throws SQLException {
        return repairJobRepository.findAll();
    }

    // Finds one repair job by ID.
    public RepairJob getRepairJobById(int id) throws SQLException {
        return repairJobRepository.findById(id);
    }

    // Searches repair jobs or returns all jobs for a blank query.
    public List<RepairJob> searchRepairJobs(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return repairJobRepository.findAll();
        }
        return repairJobRepository.search(query.trim());
    }

    // Updates the price assigned to a repair job.
    public RepairJob setRepairCost(int jobId, BigDecimal repairCost) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        job.setRepairCost(paymentRepository.setRepairCost(jobId, repairCost));
        return job;
    }

    // Records a payment for an existing repair job.
    public Payment recordPayment(int jobId, BigDecimal amount, String paymentMethod, String paymentStatus) throws SQLException {
        if (repairJobRepository.findById(jobId) == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        Payment payment = new Payment(0, jobId, amount, paymentMethod, paymentStatus, LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // Returns payment totals and status for a repair job.
    public PaymentRepository.FinanceSnapshot getFinanceSnapshot(int jobId) throws SQLException {
        if (repairJobRepository.findById(jobId) == null) {
            throw new IllegalArgumentException("Repair job not found");
        }
        return paymentRepository.getFinanceSnapshot(jobId);
    }

    // Updates whether the repair was fixed.
    public RepairJob updateRepairOutcome(int jobId, String repairOutcome) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }

        job.setRepairOutcome(repairOutcome);
        repairJobRepository.updateRepairOutcome(jobId, job.getRepairOutcome());
        return repairJobRepository.findById(jobId);
    }

    // Updates the payment status of a repair job.
    public RepairJob updatePaymentStatus(int jobId, String paymentStatus) throws SQLException {
        RepairJob job = repairJobRepository.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair job not found");
        }

        job.setPaymentStatus(paymentStatus);
        repairJobRepository.updatePaymentStatus(jobId, job.getPaymentStatus());
        return repairJobRepository.findById(jobId);
    }

    // Updates the reported fault and repair notes.
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

    // Validates and saves a repair status change.
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
            case READY_FOR_COLLECTION -> newStatus == RepairStatus.IN_PROGRESS
                    || newStatus == RepairStatus.COMPLETED;
            case COMPLETED -> newStatus == RepairStatus.IN_PROGRESS
                    || newStatus == RepairStatus.READY_FOR_COLLECTION;
        };

        if (!validTransition) {
            throw new InvalidRepairStatusException(
                    "Invalid status transition from " + previous + " to " + newStatus
                            + ". Completed repairs can be changed back to Ready for collection."
            );
        }

        job.updateStatus(newStatus);
        repairJobRepository.updateStatus(jobId, previous, newStatus, notes);
        return job;
    }

    // Returns the status history for a repair job.
    public List<Map<String, Object>> getRepairHistory(int repairJobId) throws SQLException {
        return repairHistoryRepository.findByRepairJobId(repairJobId);
    }

    // Builds counts used by the dashboard.
    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", repairJobRepository.countAll());
        stats.put("jobsInProgress", repairJobRepository.countByStatus(RepairStatus.IN_PROGRESS));
        stats.put("jobsReadyForCollection", repairJobRepository.countByStatus(RepairStatus.READY_FOR_COLLECTION));
        stats.put("completedJobs", repairJobRepository.countByStatus(RepairStatus.COMPLETED));
        return stats;
    }

    // Lists statuses allowed from the current status.
    public List<String> getAllowedNextStatuses(RepairStatus status) {
        List<String> statuses = new ArrayList<>();
        if (status == RepairStatus.IN_PROGRESS) {
            statuses.add(RepairStatus.READY_FOR_COLLECTION.name());
        } else if (status == RepairStatus.READY_FOR_COLLECTION) {
            statuses.add(RepairStatus.IN_PROGRESS.name());
            statuses.add(RepairStatus.COMPLETED.name());
        } else if (status == RepairStatus.COMPLETED) {
            statuses.add(RepairStatus.IN_PROGRESS.name());
            statuses.add(RepairStatus.READY_FOR_COLLECTION.name());
        }
        return statuses;
    }

    // Returns repair jobs through the trackable interface.
    public List<Trackable> getTrackableJobs() throws SQLException {
        List<Trackable> list = new ArrayList<>();
        for (RepairJob job : repairJobRepository.findAll()) {
            list.add(job);
        }
        return list;
    }

}
