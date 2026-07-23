package com.example.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.enums.RequestStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "elif_requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @jakarta.persistence.Lob
    private byte[] fileData;

    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "affected_no")
    private Integer affectedNo;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "is_security_risk")
    private Boolean isSecurityRisk = false;

    @Column(name = "financial_impact")
    private String financialImpact;

    public Request() {}

    public Request(User customer, String title, String description, Integer affectedNo) {
        this.customer = customer;
        this.title = title;
        this.description = description;
        this.affectedNo = affectedNo;
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime time) { this.createdAt = time; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime time) { this.updatedAt = time; }

    public Integer getAffectedNo() { return affectedNo; }
    public void setAffectedNo(Integer affectedNo) { this.affectedNo = affectedNo; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public Boolean getIsSecurityRisk() { return isSecurityRisk; }
    public void setIsSecurityRisk(Boolean isSecurityRisk) { this.isSecurityRisk = isSecurityRisk; }

    public String getFinancialImpact() { return financialImpact; }
    public void setFinancialImpact(String financialImpact) { this.financialImpact = financialImpact; }
}