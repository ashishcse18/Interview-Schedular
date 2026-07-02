package com.yourcompany.interviewscheduler.repository;

import com.yourcompany.interviewscheduler.model.entity.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<MessageTemplate, Long> {
    Optional<MessageTemplate> findByName(String name);
}
