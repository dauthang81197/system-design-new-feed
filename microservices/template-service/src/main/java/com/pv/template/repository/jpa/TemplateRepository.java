package com.pv.template.repository.jpa;

import com.pv.template.entity.TemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    Page<TemplateEntity> findAllByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM TemplateEntity t WHERE t.createdBy = :userId ORDER BY t.createdAt DESC")
    Page<TemplateEntity> findByCreatedBy(@Param("userId") String userId, Pageable pageable);
}
