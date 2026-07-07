package com.example.entities;

import com.example.enums.TaskType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "elif_prioritizations")
public class Prioritization {
    
    @Id
    private Long id; 

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "urgency", nullable = false)
    private Integer urgency;

    @Column(name = "impact", nullable = false)
    private Integer impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore;

    @Column(name = "sm_technical_score")
    private Integer smTechnicalScore;

    @Column(name = "sm_comment", length = 1000)
    private String smComment;

    @Column(name = "dev_comment", length = 1000)
    private String devComment;

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

    public Prioritization() {}
    
    public Prioritization(Request request, Integer urgency, Integer impact, TaskType taskType, Department department, Integer priorityScore) {
        this.request = request;
        this.urgency = urgency;
        this.impact = impact;
        this.taskType = taskType;
        this.department = department;
        this.priorityScore = priorityScore;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Request getRequest() { System.out.println("====== DEBUG ======= ======= GET REQUEST FROM PRI ======= ");
    System.out.println("REQUEST ID:" + request.getId()); return request; }
    
    public void setRequest(Request request) { this.request = request; }

    public Integer getUrgency() { return urgency; }
    public void setUrgency(Integer urgency) { this.urgency = urgency; }

    public Integer getImpact() { return impact; }
    public void setImpact(Integer impact) { this.impact = impact; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }

    public Integer getSmTechnicalScore() { return smTechnicalScore; }
    public void setSmTechnicalScore(Integer smTechnicalScore) { this.smTechnicalScore = smTechnicalScore; }

    public String getSmComment() { return smComment; }
    public void setSmComment(String smComment) { this.smComment = smComment; }

    public String getDevComment() { return devComment; }
    public void setDevComment(String devComment) { this.devComment = devComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}