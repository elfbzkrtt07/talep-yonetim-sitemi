package com.example.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.entities.Workflow;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.enums.WorkflowStatus;
import com.example.repositories.WorkflowRepository;
import com.example.repositories.RequestRepository;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final RequestRepository requestRepository;
    private final PrioritizationService prioritizationService;
    private final WorkflowLogService workflowLogService;

    @Autowired
    public WorkflowService(WorkflowRepository workflowRepository, RequestRepository requestRepository, 
                           PrioritizationService prioritizationService, WorkflowLogService workflowLogService) {
        this.workflowRepository = workflowRepository;
        this.requestRepository = requestRepository;
        this.prioritizationService = prioritizationService;
        this.workflowLogService = workflowLogService;
    }

    @Transactional
    public void evaluateAndAssign(Long requestId, Integer techScore, User developer, String smNotes, User currentSm) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, smNotes);

        Workflow workflow = workflowRepository.findByRequest(request)
                .orElse(new Workflow());
        
        workflow.setRequest(request);
        workflow.setCurrentAssignee(developer);
        workflow.setStatus(WorkflowStatus.DEVELOPMENT); 
        workflowRepository.save(workflow);

        workflowLogService.logAssignment(request, currentSm, developer, smNotes);
    }

    @Transactional
    public void rejectBackToPM(Long requestId, Integer techScore, String rejectionNotes, User currentSm) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, rejectionNotes);

        Workflow workflow = workflowRepository.findByRequest(request)
                .orElseThrow(() -> new IllegalArgumentException("No workflow cycle initiated for this request"));
        
        workflow.setCurrentAssignee(null); 
        workflow.setStatus(WorkflowStatus.SENT_BACK_TO_PM);
        workflowRepository.save(workflow);

        workflowLogService.logAssignment(request, currentSm, null, "Sent back to PM: " + rejectionNotes);
    }

    @Transactional
    public void updateDevStatus(Long requestId, WorkflowStatus nextStatus, String progressNotes, User developer) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        Workflow workflow = workflowRepository.findByRequest(request)
                .orElseThrow(() -> new IllegalArgumentException("No active workflow record found"));

        workflow.setStatus(nextStatus);
        workflowRepository.save(workflow);

        workflowLogService.logAssignment(request, developer, developer, progressNotes);
    }
}