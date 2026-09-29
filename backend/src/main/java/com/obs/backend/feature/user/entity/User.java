package com.obs.backend.feature.user.entity;

import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Convert;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Nullable: only ADMIN rows have one. Admin/staff log in with email;
    // customers have no email anywhere in their flow (US-007) and log in
    // with phone.
    @Column(unique = true)
    private String email;

    // Nullable: KYC fields only apply to customer registration (US-007).
    // Admin/staff rows (see V2 migration) have no ID document to record.
    @Convert(converter = StringCryptoConverter.class)
    @Column(name = "nid_number")
    private String nidNumber;

    @Column(name = "nid_expiry_date")
    private LocalDate nidExpiryDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(
            String firstName,
            String lastName,
            String passwordHash,
            String nidNumber,
            LocalDate nidExpiryDate,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            Role role,
            AccountStatus status,
            boolean phoneVerified) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.nidNumber = nidNumber;
        this.nidExpiryDate = nidExpiryDate;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.phoneVerified = phoneVerified;
    }

    public static User createStaff(
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            Role role) {
        User user = new User(
                firstName,
                lastName,
                passwordHash,
                null,
                null,
                null,
                null,
                phone,
                role,
                AccountStatus.ACTIVE,
                true);
        user.email = email;
        return user;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    /**
     * Moves the account between ACTIVE / SUSPENDED / LOCKED (US-048). Takes the target state rather
     * than exposing suspend()/lock()/reactivate() because the admin action names the state it wants
     * and every transition is permitted — there is no state machine to enforce here. Whether the
     * status permits login stays in {@link com.obs.backend.security.AccountStatusPolicy}.
     */
    public void changeStatus(AccountStatus newStatus) {
        this.status = newStatus;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void updateCustomerDetails(
            String firstName,
            String lastName,
            String phone,
            String nidNumber,
            java.time.LocalDate nidExpiryDate,
            java.time.LocalDate dateOfBirth,
            Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.nidNumber = nidNumber;
        this.nidExpiryDate = nidExpiryDate;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getNidNumber() {
        return nidNumber;
    }

    public LocalDate getNidExpiryDate() {
        return nidExpiryDate;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum Gender {
        MALE,
        FEMALE
    }
}
