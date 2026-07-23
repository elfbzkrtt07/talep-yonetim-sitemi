package com.example.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.entities.Department;
import com.example.entities.User;
import com.example.repositories.DepartmentRepository;
import com.example.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class DepartmentService {
    private DepartmentRepository departmentRepository; 
    private UserRepository userRepository;

    @Autowired
    public DepartmentService(DepartmentRepository departmentrepository, UserRepository userRepository) {
        this.departmentRepository = departmentrepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Department registerNewDepartment(Department department) {
        if (departmentRepository.findByName(department.getName()).isPresent()) {
            throw new IllegalArgumentException("A department with this name already exists.");
        }

        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Department department) {
        Department existingDepartment = departmentRepository.findById(department.getId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + department.getId()));
        
        existingDepartment.setName(department.getName());
        existingDepartment.setActive(department.isActive());
        
        return departmentRepository.save(existingDepartment);
    }
    
    public List<User> getUsersInDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + departmentId));
        
        return userRepository.findByDepartment(department);
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + id));
    }
}