package com.solutio.api.domain.blacklist.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.blacklist.domain.Blacklist;
import com.solutio.api.domain.blacklist.dto.response.BlacklistDetailResponseDto;
import com.solutio.api.domain.blacklist.dto.response.BlacklistResponseDto;
import com.solutio.api.domain.blacklist.repository.BlacklistRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.PageResponse;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final MemberRepository memberRepository;
    private final ApplicantRepository applicantRepository;

    @Transactional
    public Long addBlacklist(String studentId, String reason) {
        if (blacklistRepository.existsByStudentId(studentId)) {
            throw new GeneralException(Status.BLACKLIST_ALREADY_EXISTS);
        }

        Member member = memberRepository.findById(studentId).orElse(null);
        if (member != null) {
            Blacklist blacklist = Blacklist.create(
                member.getStudentId(), member.getEmail(), member.getDepartment(),
                member.getName(), member.getPhoneNumber(), reason
            );
            return blacklistRepository.save(blacklist).getId();
        }

        Applicant applicant = applicantRepository.findById(studentId).orElse(null);
        if (applicant != null) {
            Blacklist blacklist = Blacklist.create(
                applicant.getStudentId(), applicant.getEmail(), applicant.getDepartment(),
                applicant.getName(), applicant.getPhoneNumber(), reason
            );
            return blacklistRepository.save(blacklist).getId();
        }

        throw new GeneralException(Status.ACCOUNT_NOT_FOUND);
    }

    @Transactional
    public Long updateReason(Long id, String reason) {
        Blacklist blacklist = blacklistRepository.findById(id)
            .orElseThrow(() -> new GeneralException(Status.BLACKLIST_NOT_FOUND));
        blacklist.updateReason(reason);
        return blacklist.getId();
    }

    @Transactional
    public Long deleteBlacklist(Long id) {
        Blacklist blacklist = blacklistRepository.findById(id)
            .orElseThrow(() -> new GeneralException(Status.BLACKLIST_NOT_FOUND));
        blacklistRepository.delete(blacklist);
        return id;
    }

    public PageResponse<BlacklistResponseDto> getBlacklists(Pageable pageable) {
        Page<Blacklist> blacklists = blacklistRepository.findAll(pageable);
        return PageResponse.from(blacklists.map(BlacklistResponseDto::from));
    }

    public BlacklistDetailResponseDto getBlacklist(Long id) {
        Blacklist blacklist = blacklistRepository.findById(id)
            .orElseThrow(() -> new GeneralException(Status.BLACKLIST_NOT_FOUND));
        return BlacklistDetailResponseDto.from(blacklist);
    }
}
