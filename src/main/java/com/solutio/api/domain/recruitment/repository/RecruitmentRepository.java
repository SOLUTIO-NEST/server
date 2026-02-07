package com.solutio.api.domain.recruitment.repository;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Recruitment findFirstByIsDeleted(Boolean isDeleted);
}
