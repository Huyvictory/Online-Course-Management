package com.online.course.management.project.repository;

import com.online.course.management.project.entity.Role;
import com.online.course.management.project.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}
