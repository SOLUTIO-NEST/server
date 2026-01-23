package com.solutio.api.domain.login.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.service.ApplicantService;
import com.solutio.api.domain.login.domain.RefreshToken;
import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.dto.response.LoginResponse;
import com.solutio.api.domain.login.dto.response.TokenHeader;
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

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {
    private final MemberService memberService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final ApplicantService applicantService;

    public LoginResponse login(LoginRequestDto request) {
        String userId = request.getId();
        String password = request.getPassword();
        Member member = memberService.getUserById(userId);
        if (member != null) {
            member.isPasswordMatching(password, passwordEncoder);
            String header;

            header = TokenHeader.create(generateAndSaveToken(member)).toJson();
            return LoginResponse.create(header, false);
        }
        Applicant applicant = applicantService.getApplicantById(userId);
        if (applicant != null) {
            applicant.isPasswordMatching(password, passwordEncoder);
            String header;
            header = TokenHeader.create(generateAndSaveToken(applicant)).toJson();
            return LoginResponse.create(header, false);
        }
        throw new GeneralException(Status.ACCOUNT_NOT_FOUND);
    }

    private TokenInfo generateAndSaveToken(Member member) {

        String refreshToken = tokenProvider.generateToken(member.getStudentId(), Duration.ofDays(1), member.getRole().name());
        String accessToken = tokenProvider.generateToken(member.getStudentId(), Duration.ofHours(1), member.getRole().name());
        refreshTokenRepository.save(RefreshToken.of(member.getStudentId(), refreshToken));
        return TokenInfo.create(refreshToken, accessToken);
    }

    private TokenInfo generateAndSaveToken(Applicant applicant) {

        String refreshToken = tokenProvider.generateToken(applicant.getStudentId(), Duration.ofDays(1), Role.GUEST.name());
        String accessToken = tokenProvider.generateToken(applicant.getStudentId(), Duration.ofHours(1), Role.GUEST.name());
        refreshTokenRepository.save(RefreshToken.of(applicant.getStudentId(), refreshToken));
        return TokenInfo.create(refreshToken, accessToken);
    }

    public TokenHeader reissueToken(HttpServletRequest request) {
        String refreshToken = tokenProvider.resolveToken(request);
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new GeneralException(Status.TOKEN_NOT_FOUND));;

        refreshTokenRepository.delete(refreshTokenEntity);

        String userId = refreshTokenEntity.getUserId();
        Member member = memberService.getUserById(userId);
        if (member != null) {
            return TokenHeader.create(generateAndSaveToken(member));
        }

        Applicant applicant = applicantService.getApplicantById(userId);
        return TokenHeader.create(generateAndSaveToken(applicant));
    }
}
