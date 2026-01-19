package com.solutio.api.domain.applicant.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantService {
    private final MemberService memberService;
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<String> createMembersByRecruitment(Long recruitmentId) {
        List<Applicant> applicants = applicantRepository.findByIdRecruitmentIdAndIsApprove(recruitmentId, true);

        return applicants.stream()
            .map(this::createMemberFromApplication)
            .toList();
    }

    @Transactional
    public String createMemberByRecruitment(Long recruitmentId, String studentId) {
        Applicant applicant = applicantRepository.findByIdRecruitmentIdAndIdStudentId(recruitmentId,studentId);

        if(!applicant.getIsApprove()) {
            throw new GeneralException(Status.NOT_APPROVED_APPLICATION);
        }

        return createMemberFromApplication(applicant);
    }

    private String createMemberFromApplication(Applicant applicant) {
        Member member = memberService.createMember(applicant);
        return member.getStudentId();
    }
}
