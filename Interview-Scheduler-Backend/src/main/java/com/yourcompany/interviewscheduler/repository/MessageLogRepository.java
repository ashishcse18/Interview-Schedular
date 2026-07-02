package com.yourcompany.interviewscheduler.repository;

import com.yourcompany.interviewscheduler.model.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    List<MessageLog> findByBatchId(Long batchId);

    long countByBatchIdAndStatus(Long batchId, String status);

    void deleteByBatchId(Long batchId);

    Optional<MessageLog> findByCandidateIdAndTemplateId(Long candidateId, Long templateId);

    Optional<MessageLog> findByWhatsappMessageId(String whatsappMessageId);
}
