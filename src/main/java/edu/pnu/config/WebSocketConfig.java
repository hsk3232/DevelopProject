package edu.pnu.config;

import edu.pnu.service.security.SecurityUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration // 스프링 설정 파일임을 의미
@EnableWebSocketMessageBroker // 메시지 브로커(중계 서버)를 활성화 → Spring이 내부적으로 WebSocket 메시지를 pub/sub로 처리하게 함
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SecurityUserDetailsService securityUserDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // registerStompEnd=points 메서드
        // → 클라이언트(프론트, 브라우저, 앱 등)가 접속할 WebSocket 엔드포인트(주소) 등록

        log.info("[진입] : [WebSocketConfig] WebSocket 엔드포인트 설정을 시작합니다.");

        // JWTandshakeInterceptor를 생성하여 인터셉터로 등록
        registry.addEndpoint("/webSocket")
                .addInterceptors(new JWTHandshakeInterceptor(securityUserDetailsService))
                .setAllowedOriginPatterns("http://localhost:3000") // CORS 설정
                .withSockJS(); // SockJS 지원 활성화

        log.info("[완료] : [WebSocketConfig] '/webSocket' 엔드포인트가 설정되었습니다.");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 브로커 설정
        // 클라이언트 -> 서버 Prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 서버 -> 클라이언트 Prefix
        registry.enableSimpleBroker("/notify");
    }
}
