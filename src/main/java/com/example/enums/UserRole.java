package com.example.enums;

public enum UserRole {
    CUSTOMER("customer"),
    PRODUCT_MANAGER("pm"),
    SOFTWARE_MANAGER("sm"),
    DEVELOPER("dev"),
    ADMIN("admin");

    private final String urlSegment;

    UserRole(String urlSegment) {
        this.urlSegment = urlSegment;
    }

    public String getUrlSegment() {
        return this.urlSegment;
    }
}