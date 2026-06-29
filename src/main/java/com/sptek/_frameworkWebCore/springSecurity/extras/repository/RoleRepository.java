package com.sptek._frameworkWebCore.springSecurity.extras.repository;

import com.sptek._frameworkWebCore.springSecurity.extras.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByRoleNameIn(List<String> roleNames);
}
