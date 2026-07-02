package com.example.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.entities.WorkflowLog;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.repositories.WorkflowLogRepository;

@Service
public class WorkflowLogService {

    private final WorkflowLogRepository workflowLogRepository;

    @Autowired
    public WorkflowLogService(WorkflowLogRepository workflowLogRepository) {
        this.workflowLogRepository = workflowLogRepository;
    }

    @Transactional
    public WorkflowLog logAssignment(Request request, User assignedBy, User assignedTo, String notes) {
        WorkflowLog log = new WorkflowLog(request, assignedBy, assignedTo, notes);
        return workflowLogRepository.save(log);
    }

    public List<WorkflowLog> getLogsByRequest(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        return workflowLogRepository.findByRequestOrderByAssignedAtAsc(request);
    }
}