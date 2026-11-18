package com.solutio.api.domain.login.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.blacklist.domain.Blacklist;
import com.solutio.api.domain.blacklist.repository.BlacklistRepository;
import com.solutio.api.domain.login.domain.RefreshToken;
import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.global.auth.jwt.TokenProvider;
import com.solutio.api.global.auth.service.TokenRevocationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvcTester;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    private static final String MEMBER_STUDENT_ID = "202311111";
    private static final String APPLICANT_STUDENT_ID = "202622222";
    private static final String RAW_PASSWORD = "Password123!";

    private final Map<String, RefreshToken> tokenStore = new ConcurrentHashMap<>();
    private final Set<String> revokedJtis = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> failCounters = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        tokenStore.clear();
        revokedJtis.clear();
        failCounters.clear();

        // 로그인 Rate Limit 카운터를 인메모리로 대체 (Lua INCR+EXPIRE / GET / DELETE 에뮬레이션)
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            return keys.stream()
                    .map(key -> {
                        Long count = failCounters.get(key);
                        return count == null ? null : String.valueOf(count);
                    })
                    .toList();
        });
        given(redisTemplate.execute(ArgumentMatchers.<RedisScript<List<Long>>>any(), anyList(), any(), any(), any()))
                .willAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    List<Long> counts = new ArrayList<>();
                    for (String key : keys) {
                        counts.add(failCounters.merge(key, 1L, Long::sum));
                    }
                    return counts;
                });
        given(redisTemplate.delete(anyCollection())).willAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            long removed = 0;
            for (String key : keys) {
                if (failCounters.remove(key) != null) {
                    removed++;
                }
            }
            return removed;
        });

        willAnswer(invocation -> {
            String jti = invocation.getArgument(0);
            if (jti != null) {
                revokedJtis.add(jti);
            }
            return null;
        }).given(tokenRevocationService).revoke(anyString(), any());

        given(tokenRevocationService.isRevoked(anyString())).willAnswer(invocation -> {
            String jti = invocation.getArgument(0);
            return revokedJtis.contains(jti);
        });

        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            tokenStore.put(token.getUserId(), token);
            return token;
        });
        given(refreshTokenRepository.findByRefreshToken(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            return tokenStore.values().stream()
                    .filter(t -> token.equals(t.getRefreshToken()))
                    .findFirst();
        });
        willAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            tokenStore.remove(token.getUserId());
            return null;
        }).given(refreshTokenRepository).delete(any(RefreshToken.class));
        willAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            tokenStore.remove(userId);
            return null;
        }).given(refreshTokenRepository).deleteById(anyString());
    }

    private Member createMember(String studentId, String password) {
        Member member = Member.createFromApplicant(
                studentId,
                studentId + "@kyonggi.ac.kr",
                passwordEncoder.encode(password),
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                studentId + "_boj",
                MainLanguage.JAVA,
                ClassLevel.SEED
        );
        return memberRepository.save(member);
    }

    private Recruitment createRecruitment(LocalDateTime start, LocalDateTime end, RecruitmentStatus status) {
        Recruitment recruitment = Recruitment.create("모집공고", start, end);
        ReflectionTestUtils.setField(recruitment, "status", status);
        return recruitmentRepository.save(recruitment);
    }

    private Applicant createApplicant(String studentId, Recruitment recruitment, PassStatus passStatus, String password) {
        Applicant applicant = Applicant.create(
                studentId,
                recruitment,
                studentId + "@kyonggi.ac.kr",
                password,
                "컴퓨터공학부",
                "지원자",
                "010-5678-1234",
                studentId + "_boj",
                MainLanguage.JAVA,
                "지원동기",
                passwordEncoder
        );
        if (passStatus == PassStatus.APPROVED) {
            applicant.approve();
        } else if (passStatus == PassStatus.REJECTED) {
            applicant.reject();
        }
        return applicantRepository.save(applicant);
    }

    @Test
    @DisplayName("일반 회원(Member) 정상 로그인 성공 - 200 OK 및 토큰 정보 반환")
    void login_member_success() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        LoginRequestDto requestDto = new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();
        result.bodyJson().extractingPath("$.data.refreshToken").isNotNull();
    }

    @Test
    @DisplayName("비밀번호 불일치 시 401 UNAUTHORIZED (AUTH401) 에러 반환")
    void login_wrongPassword_returns401() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        LoginRequestDto requestDto = new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!");

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");
        result.bodyJson().extractingPath("$.message").isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 로그인 시 401 UNAUTHORIZED (AUTH401) 에러 반환 (계정 열거 방어)")
    void login_nonExistentAccount_returns401() throws Exception {
        LoginRequestDto requestDto = new LoginRequestDto("999999999", RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");
        result.bodyJson().extractingPath("$.message").isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("블랙리스트에 등록된 사용자는 로그인 차단 - 401 UNAUTHORIZED (AUTH401) 에러 반환")
    void login_blacklistedUser_returns401() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        blacklistRepository.save(Blacklist.create(
                MEMBER_STUDENT_ID, MEMBER_STUDENT_ID + "@kyonggi.ac.kr", "컴퓨터공학부",
                "홍길동", "010-1234-5678", "부정 행위"
        ));

        LoginRequestDto requestDto = new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");
        result.bodyJson().extractingPath("$.message").isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("탈퇴한 회원은 지원자 레코드가 있더라도 로그인 차단 - 401 UNAUTHORIZED (AUTH401) 에러 반환")
    void login_withdrawnMember_returns401() throws Exception {
        Recruitment recruitment = createRecruitment(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(2),
                RecruitmentStatus.CLOSED
        );
        createApplicant(MEMBER_STUDENT_ID, recruitment, PassStatus.APPROVED, RAW_PASSWORD);
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        // 회원 탈퇴 처리
        memberService.withdrawMember(MEMBER_STUDENT_ID);
        em.flush();
        em.clear();

        LoginRequestDto requestDto = new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");
        result.bodyJson().extractingPath("$.message").isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("탈퇴한 회원이 새 공고에 재지원 후 합격 시 정상적으로 재가입(재활성화)되어 로그인 성공")
    void login_withdrawnMember_reApplyAndReactivate_success() throws Exception {
        Recruitment oldRecruitment = createRecruitment(
                LocalDateTime.now().minusDays(100),
                LocalDateTime.now().minusDays(90),
                RecruitmentStatus.CLOSED
        );
        createApplicant(MEMBER_STUDENT_ID, oldRecruitment, PassStatus.APPROVED, RAW_PASSWORD);
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        // 1. 회원 탈퇴
        memberService.withdrawMember(MEMBER_STUDENT_ID);
        em.flush();
        em.clear();

        // 2. 탈퇴 직후 로그인 시도 -> 401 차단
        LoginRequestDto requestDto1 = new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD);
        assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto1)))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        // 3. 새 공고에 재지원
        Recruitment newRecruitment = createRecruitment(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                RecruitmentStatus.CLOSED
        );
        Applicant reApplicant = createApplicant(MEMBER_STUDENT_ID, newRecruitment, PassStatus.APPROVED, "NewPassword123!");
        em.flush();
        em.clear();

        // 4. 지원 기간 동안 지원자(GUEST) 권한으로 로그인 성공
        LoginRequestDto requestDto2 = new LoginRequestDto(MEMBER_STUDENT_ID, "NewPassword123!");
        var guestLoginResult = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto2)));
        guestLoginResult.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();

        // 5. 합격자 회원 전환 (Reactivate 처리)
        Member reactivatedMember = memberService.createMember(reApplicant);
        em.flush();
        em.clear();

        assertThat(reactivatedMember.getStudentId()).isEqualTo(MEMBER_STUDENT_ID);

        // 6. 재가입 회원(USER)으로 로그인 성공
        var memberLoginResult = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto2)));
        memberLoginResult.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();
    }

    @ParameterizedTest
    @EnumSource(PassStatus.class)
    @DisplayName("지원자(Applicant)는 모집 종료 후 14일 이내라면 PENDING/APPROVED/REJECTED 모두 로그인 성공 - 200 OK")
    void login_applicant_within14Days_success(PassStatus passStatus) throws Exception {
        Recruitment recruitment = createRecruitment(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                RecruitmentStatus.CLOSED
        );
        createApplicant(APPLICANT_STUDENT_ID, recruitment, passStatus, RAW_PASSWORD);

        LoginRequestDto requestDto = new LoginRequestDto(APPLICANT_STUDENT_ID, RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();
        result.bodyJson().extractingPath("$.data.refreshToken").isNotNull();
    }

    @Test
    @DisplayName("모집 종료 후 14일이 초과한 지원자는 로그인 차단 - 401 UNAUTHORIZED (AUTH401) 에러 반환")
    void login_applicant_after14Days_returns401() throws Exception {
        Recruitment recruitment = createRecruitment(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(16),
                RecruitmentStatus.CLOSED
        );
        createApplicant(APPLICANT_STUDENT_ID, recruitment, PassStatus.APPROVED, RAW_PASSWORD);

        LoginRequestDto requestDto = new LoginRequestDto(APPLICANT_STUDENT_ID, RAW_PASSWORD);

        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");
        result.bodyJson().extractingPath("$.message").isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("pair 임계치(5회) 초과 실패 후에는 올바른 비밀번호로도 429 TOO_MANY_REQUESTS(AUTH429) 차단")
    void login_pairThresholdExceeded_returns429() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        // 5회 연속 실패 -> pair 카운터가 임계치 5에 도달
        for (int i = 0; i < 5; i++) {
            assertThat(mvcTester.post().uri("/api/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!"))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        // 임계치 도달 후에는 자격 증명 확인 전에 고정 메시지로 429 응답 (잔여 시간 미노출)
        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD))));

        result.hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH429");
        result.bodyJson().extractingPath("$.message")
                .isEqualTo("너무 많은 로그인 시도가 있었습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("차단 중이라도 다른 IP에서의 시도는 pair/id 임계치 미달 시 정상 로그인 성공")
    void login_blockedPair_differentIp_success() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        for (int i = 0; i < 5; i++) {
            assertThat(mvcTester.post().uri("/api/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!"))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        // CF-Connecting-IP 헤더로 다른 클라이언트 IP에서 접속하면 pair 카운터가 분리되어 로그인 성공
        var result = assertThat(mvcTester.post().uri("/api/v1/login")
                .header("CF-Connecting-IP", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD))));

        result.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();
    }

    @Test
    @DisplayName("로그인 성공 시 3계층 실패 카운터가 모두 초기화되어 임계치 카운트가 재시작된다")
    void login_success_resetsFailCounters() throws Exception {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);

        // 4회 실패
        for (int i = 0; i < 4; i++) {
            assertThat(mvcTester.post().uri("/api/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!"))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
        assertThat(failCounters).isNotEmpty();

        // 로그인 성공 -> pair/id/ip 3계층 카운터 전부 리셋
        assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD))))
                .hasStatusOk();
        assertThat(failCounters).isEmpty();

        // 리셋 확인: 다시 4회 실패해도 429가 아닌 401 (카운트 재시작)
        for (int i = 0; i < 4; i++) {
            assertThat(mvcTester.post().uri("/api/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!"))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        // 5번째 실패로 임계치 도달 후 6번째 시도부터는 다시 429 차단
        assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, "WrongPassword!"))))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvcTester.post().uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(MEMBER_STUDENT_ID, RAW_PASSWORD))))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("정상 토큰 재발급 성공 - 200 OK 및 새 토큰 반환")
    void reissueToken_success() {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        String refreshTokenStr = tokenProvider.generateRefreshToken(MEMBER_STUDENT_ID, Duration.ofDays(1), Role.USER.name());
        refreshTokenRepository.save(RefreshToken.of(MEMBER_STUDENT_ID, refreshTokenStr));

        var result = assertThat(mvcTester.post().uri("/api/v1/login/reissue")
                .header("Authorization", "Bearer " + refreshTokenStr)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatusOk()
                .bodyJson().extractingPath("$.data.accessToken").isNotNull();
        result.bodyJson().extractingPath("$.data.refreshToken").isNotNull();
    }

    @Test
    @DisplayName("블랙리스트 사용자의 토큰 재발급 시도 시 DB 토큰 삭제 및 401 반환")
    void reissueToken_blacklistedUser_deletesTokenAndReturns401() {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        blacklistRepository.save(Blacklist.create(
                MEMBER_STUDENT_ID, MEMBER_STUDENT_ID + "@kyonggi.ac.kr", "컴퓨터공학부",
                "홍길동", "010-1234-5678", "블랙리스트 사유"
        ));
        String refreshTokenStr = tokenProvider.generateRefreshToken(MEMBER_STUDENT_ID, Duration.ofDays(1), Role.USER.name());
        refreshTokenRepository.save(RefreshToken.of(MEMBER_STUDENT_ID, refreshTokenStr));

        var result = assertThat(mvcTester.post().uri("/api/v1/login/reissue")
                .header("Authorization", "Bearer " + refreshTokenStr)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");

        assertThat(refreshTokenRepository.findByRefreshToken(refreshTokenStr)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰 재발급 시도 시 DB 토큰 삭제 및 401 반환")
    void reissueToken_withdrawnMember_deletesTokenAndReturns401() {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        String refreshTokenStr = tokenProvider.generateRefreshToken(MEMBER_STUDENT_ID, Duration.ofDays(1), Role.USER.name());
        refreshTokenRepository.save(RefreshToken.of(MEMBER_STUDENT_ID, refreshTokenStr));

        // 회원 탈퇴 처리
        memberService.withdrawMember(MEMBER_STUDENT_ID);
        em.flush();
        em.clear();

        // 탈퇴 후 토큰이 다시 생성되었다고 가정한 재발급 시도
        refreshTokenRepository.save(RefreshToken.of(MEMBER_STUDENT_ID, refreshTokenStr));

        var result = assertThat(mvcTester.post().uri("/api/v1/login/reissue")
                .header("Authorization", "Bearer " + refreshTokenStr)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");

        assertThat(refreshTokenRepository.findByRefreshToken(refreshTokenStr)).isEmpty();
    }

    @Test
    @DisplayName("지원 기간 만료된 지원자의 토큰 재발급 시도 시 DB 토큰 삭제 및 401 반환")
    void reissueToken_expiredApplicant_deletesTokenAndReturns401() {
        Recruitment recruitment = createRecruitment(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(16),
                RecruitmentStatus.CLOSED
        );
        createApplicant(APPLICANT_STUDENT_ID, recruitment, PassStatus.APPROVED, RAW_PASSWORD);
        String refreshTokenStr = tokenProvider.generateRefreshToken(APPLICANT_STUDENT_ID, Duration.ofDays(1), Role.GUEST.name());
        refreshTokenRepository.save(RefreshToken.of(APPLICANT_STUDENT_ID, refreshTokenStr));

        var result = assertThat(mvcTester.post().uri("/api/v1/login/reissue")
                .header("Authorization", "Bearer " + refreshTokenStr)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.status").isEqualTo("AUTH401");

        assertThat(refreshTokenRepository.findByRefreshToken(refreshTokenStr)).isEmpty();
    }

    @Test
    @DisplayName("정상 로그아웃 성공 - 200 OK 반환 및 RefreshToken 삭제, Access Token 블랙리스트 등록")
    void logout_success() {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        String accessToken = tokenProvider.generateAccessToken(MEMBER_STUDENT_ID, Role.USER.name());
        String refreshToken = tokenProvider.generateRefreshToken(MEMBER_STUDENT_ID, Role.USER.name());
        refreshTokenRepository.save(RefreshToken.of(MEMBER_STUDENT_ID, refreshToken));

        var result = assertThat(mvcTester.post().uri("/api/v1/login/logout")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatusOk();
        assertThat(tokenStore.containsKey(MEMBER_STUDENT_ID)).isFalse();
        String jti = tokenProvider.getJti(accessToken);
        assertThat(tokenRevocationService.isRevoked(jti)).isTrue();
    }

    @Test
    @DisplayName("로그아웃된 Access Token으로 보호된 엔드포인트 요청 시 403 FORBIDDEN 차단")
    void logout_blacklistedToken_cannotAccessProtectedEndpoint() {
        createMember(MEMBER_STUDENT_ID, RAW_PASSWORD);
        String accessToken = tokenProvider.generateAccessToken(MEMBER_STUDENT_ID, Role.USER.name());

        // 1. 로그아웃 전에는 정상 접근 가능
        assertThat(mvcTester.get().uri("/api/v1/members/me")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk();

        // 2. 로그아웃 수행
        assertThat(mvcTester.post().uri("/api/v1/login/logout")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk();

        // 3. 로그아웃 후 동일한 토큰으로 요청 시 403 FORBIDDEN 차단
        assertThat(mvcTester.get().uri("/api/v1/members/me")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 또는 잘못된 토큰으로 로그아웃 요청 시에도 멱등하게 200 OK 반환")
    void logout_idempotent_returnsOk() {
        // Authorization 헤더 없는 경우
        assertThat(mvcTester.post().uri("/api/v1/login/logout")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk();

        // 잘못된 토큰 형식인 경우
        assertThat(mvcTester.post().uri("/api/v1/login/logout")
                .header("Authorization", "Bearer invalid.jwt.token")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk();
    }
}
