package com.solutio.api.domain.applicant.repository;

import com.solutio.api.domain.applicant.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, String> {
    List<Applicant> findByRecruitmentIdAndIsApprove(Long id, Boolean isApprove);

    Applicant findByRecruitmentIdAndStudentId(Long id, String studentId);
}
