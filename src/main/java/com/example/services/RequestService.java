package com.example.services;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.repositories.PrioritizationRepository;
import com.example.repositories.RequestRepository;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public RequestService(RequestRepository requestRepository, PrioritizationRepository prioritizationRepository) {
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
    }

    @Transactional
    public Request submitRequest(Request request) {
        return requestRepository.save(request);
    }

    @Transactional
    public Request updateRequest(Request request) {
        Request existingRequest = requestRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + request.getId()));
        
        existingRequest.setAffectedNo(request.getAffectedNo());
        existingRequest.setDescription(request.getDescription());
        existingRequest.setTitle(request.getTitle());
        existingRequest.setStatus(request.getStatus()); 
        existingRequest.setUpdatedAt(java.time.LocalDateTime.now());

        return requestRepository.save(existingRequest);
    }

    @Transactional
    public void deleteRequest(Request request) {
        requestRepository.delete(request);
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }

    public Request getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
    }

    public List<Request> getRequestsByCustomer(User customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        return requestRepository.findByCustomer(customer);
    }

    @Transactional
    public void approveAndPrioritizeRequest(Request request, Prioritization prioritization) {
        Request managedRequest = entityManager.merge(request);
        
        prioritization.setRequest(managedRequest);
        prioritization.setId(managedRequest.getId()); 
        
        entityManager.persist(prioritization);
        entityManager.flush(); 
    }

    public List<Prioritization> getAllPrioritizationsSorted() {
        List<Prioritization> list = prioritizationRepository.findAll();
        list.sort((p1, p2) -> p2.getPriorityScore().compareTo(p1.getPriorityScore()));
        return list;
    }

    @Transactional
    public void convertPrioritizationToWorkflow(Prioritization prioritization) {
        Request request = prioritization.getRequest();
        
        request.setStatus(com.example.enums.RequestStatus.APPROVED); 
        entityManager.merge(request);

        com.example.entities.Workflow workflow = new com.example.entities.Workflow(
            request, 
            null,
            com.example.enums.WorkflowStatus.SUBMITTED 
        );
        
        entityManager.persist(workflow);
        entityManager.flush();
    }

    public List<com.example.entities.Workflow> getAllWorkflows() {
        return entityManager.createQuery("SELECT w FROM Workflow w JOIN FETCH w.request r JOIN FETCH r.customer", com.example.entities.Workflow.class).getResultList();
    }
}