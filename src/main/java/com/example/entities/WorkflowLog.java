package com.example.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "elif_workflow_logs")
public class WorkflowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_text", length = 2000)
    private String logText;

    @Column(name = "file_name")
    private String fileName;

    @Lob
    @Column(name = "file_bytes")
    @Basic(fetch = FetchType.EAGER)
    private byte[] fileBytes;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public WorkflowLog() {}

    public WorkflowLog(Request request, User user, String logText, String fileName, byte[] fileBytes) {
        this.request = request;
        this.user = user;
        this.logText = logText;
        this.fileName = fileName;
        this.fileBytes = fileBytes;
    }

    public WorkflowLog(Request request, User user, String logText, String fileName, byte[] fileBytes, boolean isInternal) {
        this.request = request;
        this.user = user;
        this.logText = logText;
        this.fileName = fileName;
        this.fileBytes = fileBytes;
        this.isInternal = isInternal;
    }

    public Long getId() { return id; }
    
    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLogText() { return logText; }
    public void setLogText(String logText) { this.logText = logText; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileBytes() { return fileBytes; }
    public void setFileBytes(byte[] fileBytes) { this.fileBytes = fileBytes; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean isInternal) { this.isInternal = isInternal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime time) { this.createdAt = time; }
}