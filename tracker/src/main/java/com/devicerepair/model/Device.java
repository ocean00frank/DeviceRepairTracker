package com.devicerepair.model;

public class Device {
    private int id;
    private int customerId;
    private String deviceType;
    private String brand;
    private String model;

    public Device() {
    }

    public Device(int id, int customerId, String deviceType, String brand, String model) {
        this.id = id;
        this.customerId = customerId;
        setDeviceType(deviceType);
        setBrand(brand);
        setModel(model);
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

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        if (deviceType == null || deviceType.trim().isEmpty()) {
            throw new IllegalArgumentException("Device type is required");
        }
        this.deviceType = deviceType.trim();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand is required");
        }
        this.brand = brand.trim();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model is required");
        }
        this.model = model.trim();
    }
}
