package com.service.users.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.service.users.model.AdminEntity;

public interface AdminRepository extends JpaRepository<AdminEntity, Long>{  
    boolean existsByEmail(String email);
}
