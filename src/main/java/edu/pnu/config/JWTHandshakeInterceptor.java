package edu.pnu.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import edu.pnu.service.SecurityUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JWTHandshakeInterceptor implements HandshakeInterceptor {

	private final SecurityUserDetailsService userDetailsService;
	private static final String TOKEN_PREFIX = "token=";

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {

		log.debug("[진입] : [JwtHandshakeInterceptor] WebSocket Handshake 시작. 인증을 시도합니다.");

		// 1. 헤더에서 JWT 추출
		// 1. 쿼리스트링에서 토큰 추출
		Optional<String> tokenOptional = extractTokenFromQuery(request.getURI().getQuery());

		if (tokenOptional.isEmpty()) {
			log.warn("[차단] : [JwtHandshakeInterceptor] 쿼리 파라미터에 토큰이 존재하지 않습니다.");
			return false; // 토큰 없으면 연결 거부
		}

		String token = tokenOptional.get();

		// 2. JWT 유효성 검증 및 userId 추출
		try {
			// decode()는 서명은 검증하지만, '만료 시간'은 검증하지 않음.
			DecodedJWT decodedJWT = JWT.decode(token);

			String userId = decodedJWT.getClaim("userId").asString();
			if (userId == null) {
				log.warn("[차단] : [JwtHandshakeInterceptor] 토큰에 userId 클레임이 없습니다.");
				return false;
			}

			// 3. UserDetails 조회 및 세션 속성에 저장
			UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
			attributes.put("userDetails", userDetails); // WebSocket 세션에서 사용할 사용자 정보 저장

			log.info("[성공] : [JwtHandshakeInterceptor] 사용자 [{}] 인증 성공. WebSocket 연결을 허용합니다.", userId);
			return true; // 인증 성공, 연결 허용

		} catch (JWTVerificationException e) {
			log.error("[차단] : [JwtHandshakeInterceptor] 유효하지 않은 JWT 토큰입니다. Message: {}", e.getMessage());
		} catch (Exception e) {
			log.error("[차단] : [JwtHandshakeInterceptor] 인증 처리 중 예외가 발생했습니다.", e);
		}

		return false; // 예외 발생 시 연결 거부
	}

	private Optional<String> extractTokenFromQuery(String query) {
		if (query == null || !query.contains(TOKEN_PREFIX)) {
			return Optional.empty();
		}
		return Arrays.stream(query.split("&")).filter(param -> param.startsWith(TOKEN_PREFIX)).findFirst()
				.map(param -> param.substring(TOKEN_PREFIX.length()));
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
		if (exception != null) {
			log.error("[실패] : [JwtHandshakeInterceptor] Handshake 과정에서 예외 발생.", exception);
		} else {
			log.info("[완료] : [JwtHandshakeInterceptor] Handshake 성공적으로 완료.");
		}
	}

}