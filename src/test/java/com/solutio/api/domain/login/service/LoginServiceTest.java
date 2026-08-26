package com.solutio.api.domain.login.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.applicant.service.ApplicantService;
import com.solutio.api.domain.blacklist.repository.BlacklistRepository;
import com.solutio.api.domain.login.domain.RefreshToken;
import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.dto.response.TokenInfo;
import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.global.auth.jwt.TokenProvider;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private MemberService memberService;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private ApplicantService applicantService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private static final String STUDENT_ID = "202612345";
    private static final String RAW_PASSWORD = "password123!";
    private static final String ENCODED_PASSWORD = "$2a$10$realEncodedPasswordHash...";

    private Member createMember() {
        return Member.createFromApplicant(
                STUDENT_ID,
                STUDENT_ID + "@kyonggi.ac.kr",
                ENCODED_PASSWORD,
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                STUDENT_ID + "_boj",
                MainLanguage.JAVA,
                ClassLevel.SEED
        );
    }

    private Applicant createApplicant(Recruitment recruitment, PassStatus passStatus) {
        Applicant applicant = Applicant.create(
                STUDENT_ID,
                recruitment,
                STUDENT_ID + "@kyonggi.ac.kr",
                RAW_PASSWORD,
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                STUDENT_ID + "_boj",
                MainLanguage.JAVA,
                "지원동기",
                passwordEncoder
        );
        ReflectionTestUtils.setField(applicant, "password", ENCODED_PASSWORD);
        ReflectionTestUtils.setField(applicant, "passStatus", passStatus);
        return applicant;
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTests {

        @Test
        @DisplayName("일반 회원(Member) 비밀번호 일치 시 로그인 성공")
        void login_asMember_success() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);
            Member member = createMember();

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(member);
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(tokenProvider.generateAccessToken(STUDENT_ID, Role.USER.name())).willReturn("access-token");
            given(tokenProvider.generateRefreshToken(STUDENT_ID, Role.USER.name())).willReturn("refresh-token");

            TokenInfo tokenInfo = loginService.login(request);

            assertThat(tokenInfo).isNotNull();
            assertThat(tokenInfo.getAccessToken()).isEqualTo("access-token");
            assertThat(tokenInfo.getRefreshToken()).isEqualTo("refresh-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("일반 회원(Member) 비밀번호 불일치 시 401 INVALID_CREDENTIALS 발생")
        void login_asMember_wrongPassword_throws401() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, "wrong_pw");
            Member member = createMember();

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(member);
            given(passwordEncoder.matches("wrong_pw", ENCODED_PASSWORD)).willReturn(false);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);
        }

        @ParameterizedTest
        @EnumSource(PassStatus.class)
        @DisplayName("지원자(Applicant)는 모집 종료 후 14일 이내라면 PENDING/APPROVED/REJECTED 모두 로그인 성공")
        void login_asApplicant_allPassStatuses_successWithin14Days(PassStatus status) {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);
            Recruitment recruitment = Recruitment.create(
                    "공고",
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(5)
            );
            Applicant applicant = createApplicant(recruitment, status);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(applicant);
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(tokenProvider.generateAccessToken(STUDENT_ID, Role.GUEST.name())).willReturn("guest-access-token");
            given(tokenProvider.generateRefreshToken(STUDENT_ID, Role.GUEST.name())).willReturn("guest-refresh-token");

            TokenInfo tokenInfo = loginService.login(request);

            assertThat(tokenInfo).isNotNull();
            assertThat(tokenInfo.getAccessToken()).isEqualTo("guest-access-token");
            assertThat(tokenInfo.getRefreshToken()).isEqualTo("guest-refresh-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("지원자(Applicant) 비밀번호 불일치 시 401 INVALID_CREDENTIALS 발생")
        void login_asApplicant_wrongPassword_throws401() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, "wrong_pw");
            Recruitment recruitment = Recruitment.create(
                    "공고",
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(5)
            );
            Applicant applicant = createApplicant(recruitment, PassStatus.PENDING);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(applicant);
            given(passwordEncoder.matches("wrong_pw", ENCODED_PASSWORD)).willReturn(false);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("모집 종료 후 14일 초과한 지원자는 로그인 시 더미 해시 연산 후 401 INVALID_CREDENTIALS 발생")
        void login_asApplicant_expired_throws401_andExecutesDummyHash() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);
            Recruitment recruitment = Recruitment.create(
                    "공고",
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now().minusDays(16)
            );
            Applicant applicant = createApplicant(recruitment, PassStatus.APPROVED);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(applicant);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(passwordEncoder).matches(RAW_PASSWORD, LoginService.DUMMY_PASSWORD_HASH);
        }

        @Test
        @DisplayName("블랙리스트에 등록된 사용자는 로그인 시 더미 해시 연산 후 401 INVALID_CREDENTIALS 발생")
        void login_whenBlacklisted_throws401_andExecutesDummyHash() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(true);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(passwordEncoder).matches(RAW_PASSWORD, LoginService.DUMMY_PASSWORD_HASH);
        }

        @Test
        @DisplayName("탈퇴한 회원이 재지원하지 않은 경우 로그인 시 더미 해시 연산 후 401 INVALID_CREDENTIALS 발생")
        void login_whenWithdrawnMember_notReApplied_throws401_andExecutesDummyHash() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(null);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(passwordEncoder).matches(RAW_PASSWORD, LoginService.DUMMY_PASSWORD_HASH);
        }

        @Test
        @DisplayName("탈퇴한 회원이 새로운 공고에 재지원한 경우 결과 조회 기간 내라면 GUEST 로그인 성공")
        void login_whenWithdrawnMember_reApplied_success() {
            LoginRequestDto request = new LoginRequestDto(STUDENT_ID, RAW_PASSWORD);
            Recruitment recruitment = Recruitment.create(
                    "새 공고",
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(5)
            );
            Applicant reApplicant = createApplicant(recruitment, PassStatus.PENDING);

            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(reApplicant);
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(tokenProvider.generateAccessToken(STUDENT_ID, Role.GUEST.name())).willReturn("guest-access-token");
            given(tokenProvider.generateRefreshToken(STUDENT_ID, Role.GUEST.name())).willReturn("guest-refresh-token");

            TokenInfo tokenInfo = loginService.login(request);

            assertThat(tokenInfo).isNotNull();
            assertThat(tokenInfo.getAccessToken()).isEqualTo("guest-access-token");
            assertThat(tokenInfo.getRefreshToken()).isEqualTo("guest-refresh-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 계정은 더미 해시 연산 후 401 INVALID_CREDENTIALS 발생 (계정 열거 방어)")
        void login_whenAccountNotFound_throws401_andExecutesDummyHash() {
            LoginRequestDto request = new LoginRequestDto("unknown999", RAW_PASSWORD);

            given(blacklistRepository.existsByStudentId("unknown999")).willReturn(false);
            given(memberService.getUserById("unknown999")).willReturn(null);
            given(applicantService.getApplicantById("unknown999")).willReturn(null);

            assertThatThrownBy(() -> loginService.login(request))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(passwordEncoder).matches(RAW_PASSWORD, LoginService.DUMMY_PASSWORD_HASH);
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTests {

        private static final String VALID_REFRESH_TOKEN = "valid-refresh-token";

        @Test
        @DisplayName("일반 회원(Member) 토큰 재발급 성공 시 기존 토큰 삭제 및 신규 발급")
        void reissueToken_asMember_success() {
            RefreshToken tokenEntity = RefreshToken.of(STUDENT_ID, VALID_REFRESH_TOKEN);
            Member member = createMember();

            given(tokenProvider.resolveToken(httpServletRequest)).willReturn(VALID_REFRESH_TOKEN);
            given(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(refreshTokenRepository.findByRefreshToken(VALID_REFRESH_TOKEN)).willReturn(Optional.of(tokenEntity));
            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(member);
            given(tokenProvider.generateAccessToken(STUDENT_ID, Role.USER.name())).willReturn("new-access-token");
            given(tokenProvider.generateRefreshToken(STUDENT_ID, Role.USER.name())).willReturn("new-refresh-token");

            TokenInfo result = loginService.reissueToken(httpServletRequest);

            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
            verify(refreshTokenRepository).delete(tokenEntity);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("블랙리스트에 등록된 사용자가 토큰 재발급 시도 시 DB에서 토큰 삭제 후 401 반환")
        void reissueToken_whenBlacklisted_deletesTokenAndThrows401() {
            RefreshToken tokenEntity = RefreshToken.of(STUDENT_ID, VALID_REFRESH_TOKEN);

            given(tokenProvider.resolveToken(httpServletRequest)).willReturn(VALID_REFRESH_TOKEN);
            given(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(refreshTokenRepository.findByRefreshToken(VALID_REFRESH_TOKEN)).willReturn(Optional.of(tokenEntity));
            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(true);

            assertThatThrownBy(() -> loginService.reissueToken(httpServletRequest))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(refreshTokenRepository).delete(tokenEntity);
        }

        @Test
        @DisplayName("탈퇴한 회원이 토큰 재발급 시도 시 DB에서 토큰 삭제 후 401 반환")
        void reissueToken_whenWithdrawnMember_deletesTokenAndThrows401() {
            RefreshToken tokenEntity = RefreshToken.of(STUDENT_ID, VALID_REFRESH_TOKEN);

            given(tokenProvider.resolveToken(httpServletRequest)).willReturn(VALID_REFRESH_TOKEN);
            given(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(refreshTokenRepository.findByRefreshToken(VALID_REFRESH_TOKEN)).willReturn(Optional.of(tokenEntity));
            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(null);

            assertThatThrownBy(() -> loginService.reissueToken(httpServletRequest))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(refreshTokenRepository).delete(tokenEntity);
        }

        @Test
        @DisplayName("지원 기간 만료된 지원자가 토큰 재발급 시도 시 DB에서 토큰 삭제 후 401 반환")
        void reissueToken_whenApplicantExpired_deletesTokenAndThrows401() {
            RefreshToken tokenEntity = RefreshToken.of(STUDENT_ID, VALID_REFRESH_TOKEN);
            Recruitment recruitment = Recruitment.create(
                    "공고",
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now().minusDays(16)
            );
            Applicant applicant = createApplicant(recruitment, PassStatus.APPROVED);

            given(tokenProvider.resolveToken(httpServletRequest)).willReturn(VALID_REFRESH_TOKEN);
            given(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(refreshTokenRepository.findByRefreshToken(VALID_REFRESH_TOKEN)).willReturn(Optional.of(tokenEntity));
            given(blacklistRepository.existsByStudentId(STUDENT_ID)).willReturn(false);
            given(memberService.getUserById(STUDENT_ID)).willReturn(null);
            given(applicantService.getApplicantById(STUDENT_ID)).willReturn(applicant);

            assertThatThrownBy(() -> loginService.reissueToken(httpServletRequest))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(refreshTokenRepository).delete(tokenEntity);
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 토큰 재발급 시도 시 DB에서 토큰 삭제 후 401 반환")
        void reissueToken_whenAccountNotFound_deletesTokenAndThrows401() {
            RefreshToken tokenEntity = RefreshToken.of("unknown999", VALID_REFRESH_TOKEN);

            given(tokenProvider.resolveToken(httpServletRequest)).willReturn(VALID_REFRESH_TOKEN);
            given(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(tokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
            given(refreshTokenRepository.findByRefreshToken(VALID_REFRESH_TOKEN)).willReturn(Optional.of(tokenEntity));
            given(blacklistRepository.existsByStudentId("unknown999")).willReturn(false);
            given(memberService.getUserById("unknown999")).willReturn(null);
            given(applicantService.getApplicantById("unknown999")).willReturn(null);

            assertThatThrownBy(() -> loginService.reissueToken(httpServletRequest))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getStatus())
                    .isEqualTo(Status.INVALID_CREDENTIALS);

            verify(refreshTokenRepository).delete(tokenEntity);
        }
    }
}
