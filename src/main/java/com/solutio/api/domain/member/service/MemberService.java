package com.solutio.api.domain.member.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member createMember(Applicant applicant) {

        Member member = memberRepository.findById(applicant.getStudentId()).orElse(null);
        if(member != null) return member;

        member = Member.create(
            applicant.getStudentId(),
            applicant.getEmail(),
            applicant.getPassword(),
            applicant.getDepartment(),
            applicant.getName(),
            applicant.getPhoneNumber(),
            applicant.getBojId(),
            applicant.getMainLanguage(),
            passwordEncoder
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
}
