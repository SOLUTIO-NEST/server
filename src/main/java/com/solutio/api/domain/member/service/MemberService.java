package com.solutio.api.domain.member.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.dto.request.MemberUpdateRequestDto;
import com.solutio.api.domain.member.dto.response.MemberMyInfoResponseDto;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final ApplicantRepository applicantRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Member createMember(Applicant applicant) {
        Member member = memberRepository.findById(applicant.getStudentId()).orElse(null);

        if (member != null) {
            return member;
        }

        Optional<Member> withdrawnMember = memberRepository.findWithdrawnByStudentId(applicant.getStudentId());
        if (withdrawnMember.isPresent()) {
            Member existingMember = withdrawnMember.get();
            existingMember.reactivate(
                    applicant.getEmail(),
                    applicant.getPassword(),
                    applicant.getDepartment(),
                    applicant.getName(),
                    applicant.getPhoneNumber(),
                    applicant.getBojId(),
                    applicant.getMainLanguage(),
                    applicant.getClassLevel()
            );
            return existingMember;
        }

        member = Member.createFromApplicant(
            applicant.getStudentId(),
            applicant.getEmail(),
            applicant.getPassword(),
            applicant.getDepartment(),
            applicant.getName(),
            applicant.getPhoneNumber(),
            applicant.getBojId(),
            applicant.getMainLanguage(),
            applicant.getClassLevel()
            );

        return memberRepository.save(member);
    }

    public Member getUserById(String userId) {
        return memberRepository.findById(userId).orElse(null);
    }

    public String getMyUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ((UserDetails)principal).getUsername();
    }

    public MemberMyInfoResponseDto getMyInfo() {
        String studentId = getMyUserId();
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new GeneralException(Status.ACCOUNT_NOT_FOUND));
        return MemberMyInfoResponseDto.from(member);
    }

    @Transactional
    public void updateMyInfo(MemberUpdateRequestDto requestDto) {
        String studentId = getMyUserId();
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new GeneralException(Status.ACCOUNT_NOT_FOUND));
        member.updateMyInfo(requestDto);
    }

    @Transactional
    public void withdraw() {
        String studentId = getMyUserId();
        withdrawMember(studentId);
    }

    @Transactional
    public void withdrawMember(String studentId) {
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new GeneralException(Status.ACCOUNT_NOT_FOUND));
        member.delete();
        applicantRepository.deleteById(studentId);
        refreshTokenRepository.deleteById(studentId);
    }
}
