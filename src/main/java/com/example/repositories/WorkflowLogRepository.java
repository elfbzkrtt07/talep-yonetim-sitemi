package com.example.repositories;

import com.example.entities.WorkflowLog;
import com.example.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {

    @Query("SELECT w FROM WorkflowLog w " +
           "JOIN FETCH w.user " +
           "WHERE w.request.id = :requestId " +
           "ORDER BY w.createdAt ASC")
    List<WorkflowLog> findLogsByRequestId(@Param("requestId") Long requestId);
}