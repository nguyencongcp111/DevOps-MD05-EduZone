package com.eduzone.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Học viên và số dư ví (VND). */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "wallet_balance", nullable = false)
    private Long walletBalance;

    protected Student() {
    }

    public Student(String fullName, String email, Long walletBalance) {
        this.fullName = fullName;
        this.email = email;
        this.walletBalance = walletBalance;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Long getWalletBalance() { return walletBalance; }
}
