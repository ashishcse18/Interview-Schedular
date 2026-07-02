package com.yourcompany.interviewscheduler.repository;

import com.yourcompany.interviewscheduler.model.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByBatchId(Long batchId);

    void deleteByBatchId(Long batchId);
}
