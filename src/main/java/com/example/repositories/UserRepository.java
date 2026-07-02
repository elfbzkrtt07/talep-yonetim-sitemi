package com.example.repositories;

import com.example.entities.Company;
import com.example.entities.Department;
import com.example.entities.User;
import com.example.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByCompany(Company company);
    List<User> findByDepartment(Department department);
    boolean existsByEmail(String email);
    Optional<User> findById(Long id);
    @Query(value = "SELECT * FROM elif_users WHERE role = :role AND department_id = :deptId", nativeQuery = true)
    List<User> findByRoleAndDepartmentId(@Param("role") String role, @Param("deptId") Long deptId);
}