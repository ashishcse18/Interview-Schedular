package com.yourcompany.interviewscheduler.repository;

import com.yourcompany.interviewscheduler.model.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
}
