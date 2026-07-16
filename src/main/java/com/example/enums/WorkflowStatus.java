package com.example.enums;

public enum WorkflowStatus {
    SUBMITTED,          // Freshly filed by Customer
    UNDER_REVIEW,       // PM is actively evaluating & configuring matrix rules
    APPROVED_BY_PM,     // PM pushed it to the technical department pool
    DEVELOPMENT,        // SM assigned a dev; developer is coding it
    COMPLETED,          // Done, closed, deployed

    SENT_BACK_TO_PM,    // SM rejected technical feasibility / sent it back to PM
    SENT_BACK_TO_SM,   // QA or SM rejected the developer's work, sent back to code
    REVISION_REQUIRED;   // PM sent it all the way back to the Customer for clearer text

   @Override
    public String toString() {
        switch (this) {
            case SUBMITTED: return "Talep Kaydedildi";
            case UNDER_REVIEW: return "Talep Değerlendirme Aşamasında";
            case APPROVED_BY_PM: return "Ürün Yöneticisi Tarafından Onaylandı";
            case DEVELOPMENT: return "Yazılım Aşamasında";
            case COMPLETED: return "Tamamlandı";
            case SENT_BACK_TO_PM: return "Ürün Yöneticisine Geri Gönderildi";
            case SENT_BACK_TO_SM: return "Yazılım Yöneticisine Geri Gönderildi";
            case REVISION_REQUIRED: return "Düzenleme Gerekiyor";
            default: return "";
        }
    }
}