package com.solutio.api.domain.applicant.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.dto.request.ApplicantCreateRequestDto;
import com.solutio.api.domain.applicant.dto.response.ApplicantPassResponseDto;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.recruitment.service.RecruitmentService;
import com.solutio.api.domain.recruitment.domain.Recruitment;
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
    private final RecruitmentService recruitmentService;
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String applyMember(ApplicantCreateRequestDto requestDto) {

        Recruitment recruitment = recruitmentService.getRecruitment(requestDto.getRecruitmentId());

        Applicant applicant = Applicant.create(
            requestDto.getStudentId(),
            recruitment,
            requestDto.getEmail(),
            requestDto.getPassword(),
            requestDto.getDepartment(),
            requestDto.getName(),
            requestDto.getPhoneNumber(),
            requestDto.getBojId(),
            requestDto.getMainLanguage(),
            requestDto.getApplyReason(),
            passwordEncoder
        );

        return applicantRepository.save(applicant).getStudentId();
    }

    @Transactional
    public List<String> createMembersByRecruitment(Long recruitmentId) {
        List<Applicant> applicants = applicantRepository.findByRecruitmentIdAndIsApprove(recruitmentId, true);

        return applicants.stream()
            .map(this::createMemberFromApplicant)
            .toList();
    }

    @Transactional
    public String createMemberByRecruitment(String studentId) {
        Applicant applicant = applicantRepository.findById(studentId)
            .orElseThrow(() -> new GeneralException(Status.APPLICANT_NOT_FOUND));

        if(!applicant.getIsApprove()) {
            throw new GeneralException(Status.NOT_APPROVED_APPLICANT);
        }

        return createMemberFromApplicant(applicant);
    }

    private String createMemberFromApplicant(Applicant applicant) {
        Member member = memberService.createMember(applicant);
        return member.getStudentId();
    }

    public Applicant getApplicantById(String userId) {
        return applicantRepository.findById(userId).orElse(null);
    }

    @Transactional
    public String approveApplicant(String studentId) {
        Applicant applicant = applicantRepository.findById(studentId)
            .orElseThrow(() -> new GeneralException(Status.APPLICANT_NOT_FOUND));

        applicant.approve();

        return applicant.getStudentId();
    }

    @Transactional
    public String rejectApplicant(String studentId) {
        Applicant applicant = applicantRepository.findById(studentId)
            .orElseThrow(() -> new GeneralException(Status.APPLICANT_NOT_FOUND));

        applicant.reject();

        return applicant.getStudentId();
    }

    public ApplicantPassResponseDto checkPassStatus() {
        String studentId = memberService.getMyUserId();
        Applicant applicant = applicantRepository.findById(studentId)
            .orElseThrow(() -> new GeneralException(Status.APPLICANT_NOT_FOUND));

        return ApplicantPassResponseDto.from(applicant);
    }
}
