package com.example.repositories;

import java.util.List;

import com.example.entities.Request;
import com.example.entities.User;
import com.example.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByCustomer(User customer);
    List<Request> findByStatus(RequestStatus status);

    @Modifying
    @Query("UPDATE Request r SET r.status = :status WHERE r.id = :reqId")
    void updateRequestStatus(@Param("reqId") Long requestId, @Param("status") String status);
}
