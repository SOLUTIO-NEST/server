package com.solutio.api.domain.login.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.service.ApplicantService;
import com.solutio.api.domain.blacklist.repository.BlacklistRepository;
import com.solutio.api.domain.login.domain.RefreshToken;
import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.dto.response.TokenInfo;
import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.global.auth.jwt.TokenProvider;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    public static final String DUMMY_PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    private final MemberService memberService;
    private final BlacklistRepository blacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final ApplicantService applicantService;

    @Transactional
    public TokenInfo login(LoginRequestDto request) {
        String userId = request.getId();
        String password = request.getPassword();

        if (blacklistRepository.existsByStudentId(userId)) {
            passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            throw new GeneralException(Status.INVALID_CREDENTIALS);
        }

        Member member = memberService.getUserById(userId);
        if (member != null) {
            if (!passwordEncoder.matches(password, member.getPassword())) {
                throw new GeneralException(Status.INVALID_CREDENTIALS);
            }
            return generateAndSaveToken(member);
        }

        Applicant applicant = applicantService.getApplicantById(userId);
        if (applicant != null) {
            if (applicant.isLoginPeriodExpired()) {
                passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
                throw new GeneralException(Status.INVALID_CREDENTIALS);
            }
            if (!passwordEncoder.matches(password, applicant.getPassword())) {
                throw new GeneralException(Status.INVALID_CREDENTIALS);
            }
            return generateAndSaveToken(applicant);
        }

        passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
        throw new GeneralException(Status.INVALID_CREDENTIALS);
    }

    private TokenInfo generateAndSaveToken(Member member) {
        String refreshToken = tokenProvider.generateRefreshToken(member.getStudentId(), member.getRole().name());
        String accessToken = tokenProvider.generateAccessToken(member.getStudentId(), member.getRole().name());
        refreshTokenRepository.save(RefreshToken.of(member.getStudentId(), refreshToken));
        return TokenInfo.create(accessToken, refreshToken);
    }

    private TokenInfo generateAndSaveToken(Applicant applicant) {
        String refreshToken = tokenProvider.generateRefreshToken(applicant.getStudentId(), Role.GUEST.name());
        String accessToken = tokenProvider.generateAccessToken(applicant.getStudentId(), Role.GUEST.name());
        refreshTokenRepository.save(RefreshToken.of(applicant.getStudentId(), refreshToken));
        return TokenInfo.create(accessToken, refreshToken);
    }

    @Transactional
    public TokenInfo reissueToken(HttpServletRequest request) {
        String refreshToken = tokenProvider.resolveToken(request);
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new GeneralException(Status.TOKEN_NOT_FOUND);
        }
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new GeneralException(Status.TOKEN_NOT_FOUND));

        String userId = refreshTokenEntity.getUserId();

        if (blacklistRepository.existsByStudentId(userId)) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new GeneralException(Status.INVALID_CREDENTIALS);
        }

        Member member = memberService.getUserById(userId);
        if (member != null) {
            refreshTokenRepository.delete(refreshTokenEntity);
            return generateAndSaveToken(member);
        }

        Applicant applicant = applicantService.getApplicantById(userId);
        if (applicant != null) {
            if (applicant.isLoginPeriodExpired()) {
                refreshTokenRepository.delete(refreshTokenEntity);
                throw new GeneralException(Status.INVALID_CREDENTIALS);
            }
            refreshTokenRepository.delete(refreshTokenEntity);
            return generateAndSaveToken(applicant);
        }

        refreshTokenRepository.delete(refreshTokenEntity);
        throw new GeneralException(Status.INVALID_CREDENTIALS);
    }
}
