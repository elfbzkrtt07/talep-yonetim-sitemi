package com.example.services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.entities.WorkflowLog;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.repositories.RequestRepository;
import com.example.repositories.WorkflowLogRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class WorkflowLogService {

    private final WorkflowLogRepository workflowLogRepository;
    private final RequestRepository requestRepository;

    public WorkflowLogService(WorkflowLogRepository workflowLogRepository, RequestRepository requestRepository) {
        this.workflowLogRepository = workflowLogRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowLog> getChatHistoryForRequest(Long requestId) {
        return workflowLogRepository.findLogsByRequestId(requestId);
    }

    @Transactional
    public WorkflowLog logAssignment(Request request, User user, String logText, String fileName, byte[] fileBytes) {
        WorkflowLog log = new WorkflowLog(request, user, logText, fileName, fileBytes);
        return workflowLogRepository.save(log);
    }

    @Transactional
    public void saveChatComment(Long requestId, String commentText, User sender, String fileName, byte[] fileBytes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with ID: " + requestId));

        WorkflowLog log = new WorkflowLog();
        log.setRequest(request);
        log.setUser(sender);
        
        log.setLogText(commentText != null ? commentText.trim() : null);
        
        log.setFromStatus(request.getStatus().name());
        log.setToStatus(request.getStatus().name());

        if (fileBytes != null && fileName != null) {
            log.setFileName(fileName);
            log.setFileBytes(fileBytes);
        }

        boolean isInternal = true;
        if (commentText != null && (commentText.contains("[MÜŞTERİYE İADE EDİLDİ]") || (sender != null && "CUSTOMER".equals(sender.getRole().name())))) {
            isInternal = false;
        }
        log.setInternal(isInternal);

        workflowLogRepository.save(log);
    }
}