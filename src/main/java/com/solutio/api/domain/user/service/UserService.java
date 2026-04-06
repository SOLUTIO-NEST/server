package com.solutio.api.domain.user.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.user.dto.response.UserMyInfoResponseDto;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final MemberRepository memberRepository;
    private final ApplicantRepository applicantRepository;
    private final MemberService memberService;

    public UserMyInfoResponseDto getMyInfo() {
        String studentId = memberService.getMyUserId();
        boolean isGuest = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));

        if (isGuest) {
            Applicant applicant = applicantRepository.findById(studentId)
                    .orElseThrow(() -> new GeneralException(Status.APPLICANT_NOT_FOUND));
            return UserMyInfoResponseDto.from(applicant);
        }

        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new GeneralException(Status.ACCOUNT_NOT_FOUND));
        return UserMyInfoResponseDto.from(member);
    }
}
