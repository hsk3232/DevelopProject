package edu.pnu.config;

import edu.pnu.config.filter.JWTAuthenticationFilter;
import edu.pnu.config.filter.JWTAuthorizationFilter;
import edu.pnu.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// --------- Security 보안 규칙, 필터, 인가 정책 ---------//

@Configuration
public class SecurityConfig {
    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;
    @Autowired
    private MemberRepository memberRepository;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 보호 비활성화
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/webSocket/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

        );
        // Form을 이용한 로그인을 사용하지 않겠다는 명시적 설정
        // UsernamePasswordAuthenticationFilter가 현재 없지만 명시적 제거
        http.formLogin(frmLogin -> frmLogin.disable());

        // 인가되지 않은 사용자는 /unauth로 이동 -> JWT에서는 잘 안된다고 함.
        http.exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"접근 권한이 없습니다.\"}");
                })
        );

        // Http Basic인증 방식을 사용하지 않겠다는 명시적 설정
        // BasicAuthenticationFilter가 현재 없지만 명시적 제거
        http.httpBasic(basic -> basic.disable());
        // 세션을 유지하지 않겠다고 설정 (SessionManagementFilter 등록)
        // Url 호출 뒤 응답할 때 까지는 유지되지만 응답 후 삭제
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 스프링 시큐리티의 필터체인에 작성한 필터를 추가한다. UsernamePasswordAuthenticationFilter를 상속한 필터이므로
        // 원래 UsernamePasswordAuthenticationFilter가 위치하는 곳에 대신 추가된다.  //토큰에 공장 정보 넣기 위해 Repo 주입
        http.addFilter(new JWTAuthenticationFilter(authenticationConfiguration.getAuthenticationManager(), memberRepository));

        http.addFilterBefore(new JWTAuthorizationFilter(memberRepository), UsernamePasswordAuthenticationFilter.class);

        //http.addFilterBefore(new JWTAuthorizationFilter(memberRepository), AuthorizationFilter.class);

        // CORS 설정을 필터 체인에 적용
        http.cors(cors -> cors.configurationSource(corsSource()));

        System.out.println("[성공] : [2][SecurityConfig] 시큐리티 필터 체인 완성 \n");
        return http.build();
    }

    //Front 접속 허용
    private CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();

        // [프론트 서버] 요청을 허용할 주소
        config.addAllowedOriginPattern("http://localhost:3000"); // 요청을 허용할 서버
        // [분석가 서버] 요청을 허용할 주소
        config.addAllowedOriginPattern("http://10.125.121.177:8000/api/v1/barcode-anomaly-detect");

        config.addAllowedMethod(CorsConfiguration.ALL); // 요청을 허용할 Method
        config.addAllowedHeader(CorsConfiguration.ALL); // 요청을 허용할 Header
        config.setAllowCredentials(true); // 요청/응답에 자격증명정보/쿠키 포함을 허용
        // true인 경우 addAllowedOrigin("*")는 사용 불가
        // ➔ Pattern으로 변경하거나 허용되는 출처를 명시해야 함.

        // Authorization 헤더를 응답에서 노출 (JWT 등)
        config.addExposedHeader(HttpHeaders.AUTHORIZATION);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 위 설정을 적용할 Rest 서버의 URL 패턴 설정

        System.out.println("\n[성공] : [1][SecurityConfig] front 및 분석가 연결 성공 \n");
        return source;
    }

}
