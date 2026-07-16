package com.example.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Workflow;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Prioritization;
import com.example.enums.WorkflowStatus;
import com.example.repositories.WorkflowRepository;
import com.example.repositories.RequestRepository;
import com.example.repositories.PrioritizationRepository;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;
    private final PrioritizationService prioritizationService;
    private final WorkflowLogService workflowLogService;
    private final IllegalArgumentException requestNotFound = new IllegalArgumentException("Request Not Found");

    public WorkflowService(WorkflowRepository workflowRepository, 
                           RequestRepository requestRepository, 
                           PrioritizationRepository prioritizationRepository, 
                           PrioritizationService prioritizationService, 
                           WorkflowLogService workflowLogService) {
        this.workflowRepository = workflowRepository;
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.prioritizationService = prioritizationService;
        this.workflowLogService = workflowLogService;
    }

    @Transactional
    public void evaluateAndAssign(Long requestId, Integer techScore, User developer, String smNotes, User currentSm) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound);

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, smNotes);

        Workflow workflow = workflowRepository.findById(requestId)
                .orElse(new Workflow());
        
        workflow.setworkflowId(requestId);
        workflow.setRequest(request);
        workflow.setCurrentAssignee(developer);
        workflow.setStatus(WorkflowStatus.DEVELOPMENT); 
        workflowRepository.save(workflow);
    }

    @Transactional
    public void rejectBackToPM(Long requestId, Integer techScore, String rejectionNotes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> requestNotFound);

        prioritizationService.saveTechnicalEvaluation(requestId, techScore, rejectionNotes);

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

        Workflow workflow = workflowRepository.findById(requestId)
                .orElseGet(() -> {
                    Workflow newWf = new Workflow();
                    newWf.setworkflowId(requestId);
                    newWf.setRequest(request);
                    newWf.setCreatedAt(java.time.LocalDateTime.now());
                    return newWf;
                });

        workflow.setStatus(nextStatus);
        workflow.setCurrentAssignee(developer);
        workflowRepository.save(workflow);

        if (nextStatus == WorkflowStatus.COMPLETED) {
            request.setStatus(com.example.enums.RequestStatus.COMPLETED); 
            requestRepository.save(request);
        }
    }

    public List<Workflow> getCompletedJobsForDeveloper(User developer) {
        if (developer == null) {
            return java.util.Collections.emptyList();
        }
        return workflowRepository.findCompletedJobsByDeveloper(developer);
    }

    public Workflow getWorkflowByRequestId(Long requestId) {
        return workflowRepository.findById(requestId).orElse(null);
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public List<Workflow> getSentBackRequestsForPM(User currentUser) {
        return workflowRepository.findRequestsSentBackToPM();
    }

    public List<Workflow> getSentBackRequestsForSM(User currentUser) {
        List<Workflow> sentBackToSM = workflowRepository.findRequestsSentBackToSM();

        List<Workflow> sentBackToPM = workflowRepository.findRequestsSentBackToPM();

        List<Workflow> allSentBack = new ArrayList<>();
        allSentBack.addAll(sentBackToSM);
        allSentBack.addAll(sentBackToPM);

        if (currentUser != null && currentUser.getDepartment() != null) {
            Long deptId = currentUser.getDepartment().getId();
            
            return allSentBack.stream()
                .filter(w -> {
                    if (w.getRequest() == null) return false;
                    Prioritization prio = prioritizationRepository.findById(w.getRequest().getId()).orElse(null);
                    return prio != null && 
                           prio.getDepartment() != null && 
                           prio.getDepartment().getId().equals(deptId);
                })
                .toList();
        }

        return allSentBack;
    }
}