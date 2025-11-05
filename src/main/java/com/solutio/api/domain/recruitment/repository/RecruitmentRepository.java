package com.solutio.api.domain.recruitment.repository;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

}
