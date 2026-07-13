package com.example.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.entities.Company;
import com.example.entities.User;
import com.example.enums.UserRole;
import com.example.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private UserRepository userRepository;
    private final String userNotFound = "User not found with id: ";

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerNewUser(User user, UserRole role) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }
        user.setRole(role);
        user.setApproved(false); 
        return userRepository.save(user);
    }

    @Transactional
    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException( userNotFound + userId));
        user.setApproved(true);
        return userRepository.save(user);
    }

    @Transactional
    public User rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(userNotFound + userId));
        user.setApproved(false);
        sendRejectionEmail(user);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(userNotFound + user.getId()));
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPassword(user.getPassword());
        existingUser.setRole(user.getRole());
        existingUser.setCompany(user.getCompany());
        existingUser.setDepartment(user.getDepartment());
        return userRepository.save(existingUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> getUsersByCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null");
        }
        return userRepository.findByCompany(company);
    }

    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    private void sendRejectionEmail(User user) {
        // @TO-DO: Implement email sending logic here using java mail or any other email service.
    }
}
