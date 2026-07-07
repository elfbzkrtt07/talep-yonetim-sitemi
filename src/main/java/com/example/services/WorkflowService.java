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
    private final IllegalArgumentException requestNotFound = new IllegalArgumentException("Request Not Found");

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
                .orElseThrow(() -> requestNotFound);

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, smNotes);

        // Fixed here as well to maintain uniformity across the service
        Workflow workflow = workflowRepository.findById(requestId)
                .orElse(new Workflow());
        
        workflow.setworkflowId(requestId); // Explicitly ensure the shared ID is set if it's a new entity
        workflow.setRequest(request);
        workflow.setCurrentAssignee(developer);
        workflow.setStatus(WorkflowStatus.DEVELOPMENT); 
        workflowRepository.save(workflow);

        workflowLogService.logAssignment(request, currentSm, developer, smNotes);
    }

    @Transactional
    public void rejectBackToPM(Long requestId, Integer techScore, String rejectionNotes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound);

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, rejectionNotes);

        // CORRECTED: Direct primary key lookup to prevent Hibernate NonUniqueResultException
        Workflow workflow = workflowRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No workflow cycle initiated for this request"));
        
        workflow.setCurrentAssignee(null); 
        workflow.setStatus(WorkflowStatus.SENT_BACK_TO_PM);
        workflowRepository.save(workflow);
    }

    @Transactional
    public void rejectBackToSM(Long requestId, String rejectionNotes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound);

        Workflow workflow = workflowRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No workflow cycle initiated for this request"));
        
        workflow.setCurrentAssignee(null);
        workflow.setStatus(WorkflowStatus.SENT_BACK_TO_SM);
        workflowRepository.save(workflow);
    }

    @Transactional
    public void updateDevStatus(Long requestId, WorkflowStatus nextStatus, String progressNotes, User developer) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound);

        // CORRECTED: Direct primary key lookup to prevent Hibernate NonUniqueResultException
        Workflow workflow = workflowRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No active workflow record found"));

        workflow.setStatus(nextStatus);
        workflowRepository.save(workflow);

        workflowLogService.logAssignment(request, developer, developer, progressNotes);
    }
}