package edu.pnu.config.filter;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Member;
import edu.pnu.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Slf4j // ▶ 로그 출력을 위한 Slf4j
@RequiredArgsConstructor // ▶ final 멤버 필드를 파라미터로 받는 생성자 자동 생성
public class JWTAuthorizationFilter extends OncePerRequestFilter { // ▶ 요청당 한 번만 실행되는 필터 상속

    // ▶ 멤버 저장소(Repository)를 주입받음 (DB에서 사용자 정보 조회에 사용)
    private final MemberRepository memberRepository;

    // ▶ 필터가 매 요청마다 실질적으로 동작하는 메서드 (JWT 인증)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        log.info("[진입]:[1][JWTAuthorizationFilter] 토큰 확인 필터 진입");

        // [1] HTTP 요청 헤더에서 Authorization(토큰) 값 가져오기
        String srcToken = request.getHeader(HttpHeaders.AUTHORIZATION); // 요청 헤더에서 Authorization 추출
        log.info("[발행된 토큰] : {}", srcToken);

        // [2] 토큰이 없거나, 'Bearer '로 시작하지 않으면 인증처리 하지 않고 필터 통과
        if (srcToken == null || !srcToken.startsWith("Bearer ")) {
            log.info("[진입]:[2][JWTAuthorizationFilter] 토큰 없음 \n");
            filterChain.doFilter(request, response); // 다음 필터로 넘김
            return;
        }
        log.info("[완료]:[2][JWTAuthorizationFilter] 토큰 확인 완료");

        // [3] 토큰에서 'Bearer ' 부분만 제거해서 실제 JWT만 추출
        String jwtToken = srcToken.replace("Bearer ", "");

        // [4] 토큰에서 userId 추출 및 검증
        String userId = null;
        try {
            // [4-1] 토큰의 userId 클레임 추출 (예외 처리로 만료, 변조 등 검사)
            System.out.println("[진행]:[3][JWTAuthorizationFilter] username(id) 추출 시작 \n");
            userId = JWT.require(Algorithm.HMAC256("edu.pnu.jwt"))
                        .build()
                        .verify(jwtToken)
                        .getClaim("userId")
                        .asString();

            // [4-2] 토큰의 만료일시 검사 (만료되었으면 401 에러 + 토큰 재발급)
            if(JWT.require(Algorithm.HMAC256("edu.pnu.jwt"))
                  .build()
                  .verify(jwtToken)
                  .getExpiresAt()
                  .before(new Date())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                log.info("[진행]:[4][JWTAuthorizationFilter] 로그인한 아이디의 토큰 유효기간 만료");
                response.setContentType("text/plain");
                response.getWriter().write("Expired Token");

                // 만료 시 새 토큰 발급해서 헤더에 추가
                String token = JWT.create()
                                  .withExpiresAt(new Date(System.currentTimeMillis() + 1000*60*100000))
                                  .withClaim("userId", userId)
                                  .sign(Algorithm.HMAC256("edu.pnu.jwt"));

                log.info("[진행]:[4][JWTAuthorizationFilter] 로그인한 아이디의 토큰 재발행 전송 \n");
                response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                return;
            }

        } catch (Exception e) {
            // [5] 토큰 검증 실패시 (예: 만료, 변조 등) 401 에러 반환
            log.info("[오류]:[4][JWTAuthorizationFilter] JWT 오류 발생 {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return;
        }

        // [6] userId가 추출되지 않으면 인증 실패 처리
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return;
        }

        // [7] 추출한 userId로 DB에서 Member 객체 조회 (Optional 사용)
        Optional<Member> opt = memberRepository.findByUserId(userId);
        log.info("[진행]:[5][JWTAuthorizationFilter] username(id) 검색 시작");
        if (!opt.isPresent()) {
            // 사용자가 없으면 인증 실패 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            log.info("[오류]:[5][JWTAuthorizationFilter]사용자가 없다.");
            return;
        }
        log.info("[진행]:[6][JWTAuthorizationFilter] 사용자 찾음");

        // [8] DB에서 찾은 Member 정보 꺼내기
        Member findmember = opt.get();
        log.info("[진행]:[7][JWTAuthorizationFilter] ROLE 값: [{}]", findmember.getRole());

        try {
            // [9] CustomUserDetails 객체 생성 (Spring Security 인증 객체)
            CustomUserDetails customUser = CustomUserDetails.from(findmember);
           

            // [10] UsernamePasswordAuthenticationToken으로 인증 객체 생성
            Authentication auth = new UsernamePasswordAuthenticationToken(
                customUser,
                null,
                customUser.getAuthorities()
            );

            // [11] 인증 객체를 SecurityContext에 등록 (이후 컨트롤러에서 인증 정보로 활용)
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.info("[성공]:[8][JWTAuthorizationFilter] 토큰 확인 완료 \n");
            filterChain.doFilter(request, response); // 정상 인증, 다음 필터로 넘김

        } catch (Exception e) {
            // 예외 발생시 로그 출력 후 인증 실패
            e.printStackTrace();
            log.info("[오류]:[8][JWTAuthorizationFilter] 오류 발생! \n");
        }
    }
}
