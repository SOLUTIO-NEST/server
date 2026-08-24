package com.solutio.api.domain.login.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.service.ApplicantService;
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
    private final MemberService memberService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final ApplicantService applicantService;

    public TokenInfo login(LoginRequestDto request) {
        String userId = request.getId();
        String password = request.getPassword();
        Member member = memberService.getUserById(userId);
        if (member != null) {
            member.isPasswordMatching(password, passwordEncoder);
            return generateAndSaveToken(member);
        }
        Applicant applicant = applicantService.getApplicantById(userId);
        if (applicant != null) {
            applicant.isPasswordMatching(password, passwordEncoder);
            return generateAndSaveToken(applicant);
        }
        throw new GeneralException(Status.ACCOUNT_NOT_FOUND);
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

    public TokenInfo reissueToken(HttpServletRequest request) {
        String refreshToken = tokenProvider.resolveToken(request);
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new GeneralException(Status.TOKEN_NOT_FOUND);
        }
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new GeneralException(Status.TOKEN_NOT_FOUND));

        refreshTokenRepository.delete(refreshTokenEntity);

        String userId = refreshTokenEntity.getUserId();
        Member member = memberService.getUserById(userId);
        if (member != null) {
            return generateAndSaveToken(member);
        }

        Applicant applicant = applicantService.getApplicantById(userId);
        return generateAndSaveToken(applicant);
    }
}
