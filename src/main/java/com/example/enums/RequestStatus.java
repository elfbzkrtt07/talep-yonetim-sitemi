package com.example.enums;

public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED,
    SENT_BACK;

    @Override
    public String toString() {
        switch (this) {
            case PENDING: return "Onay Bekleniyor";
            case APPROVED: return "Talep Onaylandı";
            case REJECTED: return "Talep Reddedildi";
            case COMPLETED: return "Tamamlandı";
            case SENT_BACK: return "Talep Geri Gönderildi";
            default: return "";
        }
    }
}