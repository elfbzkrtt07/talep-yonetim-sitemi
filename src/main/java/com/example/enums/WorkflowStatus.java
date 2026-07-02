package com.example.enums;

public enum WorkflowStatus {
    // Standard Lifecycle Stages
    SUBMITTED,          // Freshly filed by Customer
    UNDER_REVIEW,       // PM is actively evaluating & configuring matrix rules
    APPROVED_BY_PM,     // PM pushed it to the technical department pool
    DEVELOPMENT,        // SM assigned a dev; developer is coding it
    COMPLETED,          // Done, closed, deployed

    // 🌟 Explicit "Sent Back" Loop Trackers
    SENT_BACK_TO_PM,    // SM rejected technical feasibility / sent it back to PM
    SENT_BACK_TO_DEV,   // QA or SM rejected the developer's work, sent back to code
    REVISION_REQUIRED   // PM sent it all the way back to the Customer for clearer text
}