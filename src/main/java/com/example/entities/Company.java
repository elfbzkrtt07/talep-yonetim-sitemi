package com.example.entities;

import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "elif_companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "score")
    private Double companyScore = 50.0;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<User> users;

    public Company() {}

    public Company(String name, Double companyScore) {
        this.name = name;
        this.companyScore = companyScore;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getCompanyScore() { return companyScore; }
    public void setCompanyScore(Double companyScore) { this.companyScore = companyScore; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}
