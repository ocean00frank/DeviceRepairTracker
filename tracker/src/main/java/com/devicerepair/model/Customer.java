package com.devicerepair.model;

public class Customer {
    private int id;
    private String fullName;
    private String phoneNumber;

    public Customer() {
    }

    public Customer(int id, String fullName, String phoneNumber) {
        this.id = id;
        setFullName(fullName);
        setPhoneNumber(phoneNumber);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer full name is required");
        }
        this.fullName = fullName.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        this.phoneNumber = phoneNumber.trim();
    }
}
