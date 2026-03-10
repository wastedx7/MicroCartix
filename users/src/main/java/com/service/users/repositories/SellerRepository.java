package com.service.users.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.users.model.SellerEntity;

@Repository
public interface SellerRepository extends JpaRepository<SellerEntity, Long> {
    boolean existsByEmail(String email);
    Optional<SellerEntity> findByEmail(String email);
}
