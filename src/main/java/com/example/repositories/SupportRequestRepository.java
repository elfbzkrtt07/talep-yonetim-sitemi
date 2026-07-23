package com.example.repositories;

import com.example.entities.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findBySenderId(Long senderId);

    List<SupportRequest> findBySubjectContaining(String keyword);
}