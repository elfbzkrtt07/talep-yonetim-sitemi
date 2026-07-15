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

    @Override
    public String toString() {
        
        switch(this.urlSegment) {
            case "customer":    return "Müşteri";
            case "pm":          return "Ürün Yöneticisi";
            case "sm":          return "Yazılım Yöneticisi";
            case "dev":         return "Yazılımcı";
            case "admin":       return "Admin";
            default:            return this.urlSegment;
        }
    }
}