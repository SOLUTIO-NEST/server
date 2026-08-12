package com.solutio.api.domain.applicant.repository;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, String> {
    List<Applicant> findByRecruitmentIdAndPassStatus(Long recruitmentId, PassStatus passStatus);

    Page<Applicant> findAllByRecruitmentId(Long recruitmentId, Pageable pageable);
}
