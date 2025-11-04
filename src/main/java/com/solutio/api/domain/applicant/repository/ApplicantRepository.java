package com.solutio.api.domain.applicant.repository;

import com.solutio.api.domain.applicant.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

}
