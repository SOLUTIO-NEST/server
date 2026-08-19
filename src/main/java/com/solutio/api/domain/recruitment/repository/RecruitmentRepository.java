package com.solutio.api.domain.recruitment.repository;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Recruitment findFirstByIsDeleted(Boolean isDeleted);

    Page<Recruitment> findAllByOrderByStartDateTimeDesc(Pageable pageable);

    @Query("SELECT r FROM Recruitment r " +
           "WHERE r.isApplicantDataPurged = false " +
           "AND r.announcementDateTime IS NOT NULL " +
           "AND r.announcementDateTime <= :baseDateTime")
    List<Recruitment> findAllEligibleForApplicantPurge(@Param("baseDateTime") LocalDateTime baseDateTime);
}
