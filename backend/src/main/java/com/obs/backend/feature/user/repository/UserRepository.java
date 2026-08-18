package com.obs.backend.feature.user.repository;

import com.obs.backend.feature.user.entity.User;
import com.obs.backend.security.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByPhone(String phone);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndRole(UUID id, Role role);

    long countByRole(Role role);

    // `users` holds customers and staff in one table, so every admin-facing customer query has to
    // filter by role — otherwise the bootstrap admin and every staff account US-047 creates show up
    // in the customer list. Filtering here rather than in the service keeps that impossible to forget.
    // `search` is never null — the service normalises "no search" to an empty string, which makes
    // every LIKE below '%%' and matches everything. Passing null instead would leave Postgres unable
    // to infer the parameter's type (it lands as bytea) and LOWER() would fail to resolve.
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR u.phone LIKE CONCAT('%', :search, '%'))
            """)
    Page<User> searchByRole(@Param("role") Role role, @Param("search") String search, Pageable pageable);
}
