package com.example.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.TaskType;
import com.example.enums.WorkflowStatus;
import com.example.repositories.PrioritizationRepository;
import com.example.repositories.RequestRepository;
import com.example.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PrioritizationService {

    private final PrioritizationRepository prioritizationRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;

    public PrioritizationService(PrioritizationRepository prioritizationRepository, RequestRepository requestRepository, UserRepository userRepository) {
        this.prioritizationRepository = prioritizationRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    public List<Workflow> getWorkflowsForDepartment(Long departmentId) {
        return prioritizationRepository.findUnassignedWorkflowsByDepartmentId(departmentId);
    }

    @Transactional
    public Prioritization createPrioritizationFromRequest(Long requestId, Prioritization prioritization, 
                                                        double taskTypeCoefficient, double waitTime, 
                                                        double interventionTime) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + requestId));
        
        // 1. Customer Factor
        double customerScore = 0.0;
        if (request.getCustomer() != null && request.getCustomer().getCompany() != null) {
            customerScore = request.getCustomer().getCompany().getCompanyScore();
        }
        double logInnerValue = 1.0 + (customerScore / 6.25);
        double logBase2 = Math.log(logInnerValue) / Math.log(2); 
        double customerFactor = 1.0 + logBase2;

        // 2. Base Matrix Math
        double baseScore = prioritization.getImpact() * prioritization.getUrgency() * taskTypeCoefficient * (customerFactor + waitTime + interventionTime) / 1.8;

        double affectedPointsModifier = 0.0;
        if (request.getAffectedNo() != null) {
            int affectedCount = request.getAffectedNo();
            if (affectedCount > 0 && affectedCount <= 10) affectedPointsModifier = 2.0;
            else if (affectedCount > 10 && affectedCount <= 100) affectedPointsModifier = 5.0;
            else if (affectedCount > 100 && affectedCount <= 1000) affectedPointsModifier = 10.0;
            else if (affectedCount > 1000) affectedPointsModifier = 20.0;
        }
        baseScore += affectedPointsModifier;

        int finalRoundedScore = (int) Math.round(baseScore);
        int clampedPriorityScore = Math.min(finalRoundedScore, 100);

        prioritization.setPriorityScore(clampedPriorityScore);
        prioritization.setRequest(request);

        return prioritizationRepository.save(prioritization);
    }
    
    public int calculatePreviewScore(Request request, int impact, int urgency, com.example.enums.TaskType taskType) {
        double taskTypeCoefficient = 1.0;
        if (taskType != null) {
            switch (taskType) {
                case BUG: taskTypeCoefficient = 1.5; break;
                case FEATURE_REQUEST: taskTypeCoefficient = 1.2; break;
                case CHANGE_REQUEST: taskTypeCoefficient = 1.1; break;
                case SUPPORT: taskTypeCoefficient = 1.0; break;
            }
        }

        double customerScore = 0.0;
        if (request != null && request.getCustomer() != null && request.getCustomer().getCompany() != null) {
            customerScore = request.getCustomer().getCompany().getCompanyScore();
        }
        double logInnerValue = 1.0 + (customerScore / 6.25);
        double logBase2 = Math.log(logInnerValue) / Math.log(2); 
        double customerFactor = 1.0 + logBase2;

        double waitTime = 0.5; 
        double interventionTime = 0.2;

        double baseScore = impact * urgency * taskTypeCoefficient * (customerFactor + waitTime + interventionTime)  / 1.8;

        double affectedPointsModifier = 0.0;
        if (request != null && request.getAffectedNo() != null) {
            int affectedCount = request.getAffectedNo();
            if (affectedCount > 0 && affectedCount <= 10) affectedPointsModifier = 2.0;
            else if (affectedCount > 10 && affectedCount <= 100) affectedPointsModifier = 5.0;
            else if (affectedCount > 100 && affectedCount <= 1000) affectedPointsModifier = 10.0;
            else if (affectedCount > 1000) affectedPointsModifier = 20.0;
        }
        baseScore += affectedPointsModifier;

        int finalRoundedScore = (int) Math.round(baseScore);
        return Math.min(finalRoundedScore, 100);
    }

    @Transactional
    public Prioritization saveTechnicalEvaluation(Long requestId, Integer technicalScore, String smComment) {
        Prioritization prioritization = prioritizationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Prioritization matrix record not found for request id: " + requestId));
        
        prioritization.setSmTechnicalScore(technicalScore);
        prioritization.setSmComment(smComment);
        
        return prioritizationRepository.save(prioritization);
    }

    @Transactional
    public Prioritization updatePrioritization(Prioritization prioritization) {
        Prioritization existing = prioritizationRepository.findById(prioritization.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prioritization not found with id: " + prioritization.getId()));
        
        existing.setImpact(prioritization.getImpact());
        existing.setUrgency(prioritization.getUrgency());
        existing.setPriorityScore(prioritization.getPriorityScore());
        existing.setTaskType(prioritization.getTaskType());
        existing.setDepartment(prioritization.getDepartment());
        
        existing.setSmTechnicalScore(prioritization.getSmTechnicalScore());
        existing.setSmComment(prioritization.getSmComment());
        
        return prioritizationRepository.save(existing);
    }

    public List<Prioritization> getAllPrioritizations() {
        return prioritizationRepository.findAllWithDetails(); 
    }

    public List<Prioritization> getAllUnconvertedpPrioritizations() {
        return prioritizationRepository.findAllUnconvertedWithDetails(); 
    }

    @Transactional(readOnly = true)
    public Prioritization getPrioritizationById(Long id) {
        return prioritizationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for ID: " + id));
    }

    @Transactional
    public void completeTechnicalEvaluation(Long requestId, Integer technicalScore, String smComment, User assignedDev) {
        Prioritization prioritization = prioritizationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Prioritization record not found for request id: " + requestId));
        
        prioritization.setSmTechnicalScore(technicalScore);
        prioritization.setSmComment(smComment);
        prioritizationRepository.save(prioritization);

        prioritizationRepository.updateWorkflowAssigneeAndStatus(requestId, assignedDev, WorkflowStatus.DEVELOPMENT);
    }

    @Transactional
    public void completeDeveloperComment(Long requestId, String devComment) {
        Prioritization prioritization = prioritizationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Prioritization record not found for request id: " + requestId));
        
        prioritization.setDevComment(devComment);
        prioritizationRepository.save(prioritization);

        prioritizationRepository.updateWorkflowAssigneeAndStatus(requestId, null, WorkflowStatus.SENT_BACK_TO_SM);
    }

    @Transactional
    public void completeDeveloperJob(Long requestId, User currentDev) {
        prioritizationRepository.updateWorkflowAssigneeAndStatus(requestId, currentDev, WorkflowStatus.COMPLETED);

        requestRepository.findById(requestId).ifPresent(request -> {
            request.setStatus(com.example.enums.RequestStatus.COMPLETED);
            requestRepository.save(request);
        });
    }

    @Transactional
    public void returnJobToPM(Long requestId) {
        prioritizationRepository.updateWorkflowAssigneeAndStatus(requestId, null, WorkflowStatus.SENT_BACK_TO_PM);
    }

    @Transactional
    public void returnJobToSM(Long workflowId) {
        prioritizationRepository.updateWorkflowAssigneeAndStatus(workflowId, null, WorkflowStatus.SENT_BACK_TO_SM);
    }
}