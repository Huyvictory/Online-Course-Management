package com.online.course.management.project.repository;

import com.online.course.management.project.entity.UserRole;
import com.online.course.management.project.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);
}
