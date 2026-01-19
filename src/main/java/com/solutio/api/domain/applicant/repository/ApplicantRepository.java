package com.solutio.api.domain.applicant.repository;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.ApplicantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, ApplicantId> {
    List<Applicant> findByIdRecruitmentIdAndIsApprove(Long id, Boolean isApprove);

    Applicant findByIdRecruitmentIdAndIdStudentId(Long id, String studentId);
}
