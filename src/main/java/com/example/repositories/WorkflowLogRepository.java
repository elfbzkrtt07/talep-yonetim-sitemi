package com.example.repositories;

import com.example.entities.WorkflowLog;
import com.example.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {

    List<WorkflowLog> findByRequestOrderByAssignedAtAsc(Request request);
}