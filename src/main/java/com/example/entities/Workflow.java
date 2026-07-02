package com.example.entities;

import java.time.LocalDateTime;
import com.example.enums.WorkflowStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "elif_workflows")
public class Workflow {

    // 🌟 FIX: Removed @GeneratedValue strategy. The ID will now be derived directly from the Request entity
    @Id
    @Column(name = "workflow_id")
    private Long workflowId;

    // 🌟 FIX: Changed from @ManyToOne to a strict @OneToOne relationship
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 🌟 Tells Hibernate to use the Request entity's ID value as this table's Primary Key
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private User currentAssignee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Workflow() {}

    public Workflow(Request request, User currentAssignee, WorkflowStatus status) {
        this.request = request;
        this.currentAssignee = currentAssignee;
        this.status = status;
    }

    public Long getworkflowId() { return workflowId; }
    public void setworkflowId(Long workflowId) { this.workflowId = workflowId; }

    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public User getCurrentAssignee() { return currentAssignee; }
    public void setCurrentAssignee(User currentAssignee) { this.currentAssignee = currentAssignee; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}