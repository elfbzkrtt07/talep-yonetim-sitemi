package com.example.services;

import com.example.entities.SupportRequest;
import com.example.enums.RequestStatus;
import com.example.repositories.SupportRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;

    public SupportRequestService(SupportRequestRepository supportRequestRepository) {
        this.supportRequestRepository = supportRequestRepository;
    }

    public List<SupportRequest> getAllSupportRequests() {
        return supportRequestRepository.findAll();
    }

    public List<SupportRequest> getRequestsBySender(Long senderId) {
        return supportRequestRepository.findBySenderId(senderId);
    }

    public SupportRequest saveSupportRequest(SupportRequest request) {
        return supportRequestRepository.save(request);
    }

    public Optional<SupportRequest> getSupportRequestById(Long id) {
        return supportRequestRepository.findById(id);
    }

    public void updateStatus(Long id, RequestStatus status) {
        supportRequestRepository.findById(id).ifPresent(req -> {
            req.setStatus(status);
            supportRequestRepository.save(req);
        });
    }

    public List<SupportRequest> getUserApprovalRequests() {
        return supportRequestRepository.findBySubjectContaining("[KULLANICI ONAYI]");
    }
}