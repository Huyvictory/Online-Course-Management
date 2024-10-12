package com.online.course.management.project.repository;

import com.online.course.management.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByDeletedAtIsNull();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmailWithRoles(@Param("username") String username, @Param("email") String email);
}
