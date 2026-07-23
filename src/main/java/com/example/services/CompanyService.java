package com.example.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.entities.Company;
import com.example.entities.User;
import com.example.repositories.CompanyRepository;
import com.example.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class CompanyService {
    private CompanyRepository companyRepository; 
    private UserRepository userRepository;

    @Autowired
    public CompanyService(CompanyRepository companyrepository, UserRepository userRepository) {
        this.companyRepository = companyrepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company registerNewCompany(Company company) {
        if (companyRepository.findByName(company.getName()).isPresent()) {
            throw new IllegalArgumentException("A company with this name already exists.");
        }

        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(Company company) {
        Company existingCompany = companyRepository.findById(company.getId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + company.getId()));
        
        existingCompany.setName(company.getName());
        existingCompany.setCompanyScore(company.getCompanyScore());
        existingCompany.setActive(company.isActive());
        
        return companyRepository.save(existingCompany);
    }
    
    public List<User> getUsersInCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + companyId));
        
        return userRepository.findByCompany(company);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + id));
    }
}