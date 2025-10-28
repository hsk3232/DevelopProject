package edu.pnu.config.filter;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Member;
import edu.pnu.dto.MemberDTO;
import edu.pnu.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
//@RequiredArgsConstructor => 롬복 생성자는 8080/login으로 연결되기 때문에 url 바꾸려면 직접 생성자를 선언해야함.
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter{
	// 인증 객체, fillter 객체는 컨테이너에 올리지 못함
	// 따라서 autowired 못함
	private final AuthenticationManager authenticationManager;
	private final MemberRepository memberRepo;
	
	//생성자 직접 선언
	public JWTAuthenticationFilter(AuthenticationManager authenticationManager, MemberRepository memberRepo) {
	        super(authenticationManager);
	        this.authenticationManager = authenticationManager;
	        this.memberRepo = memberRepo; // 추가!
	        // 이 줄이 있어야 /api/public/login으로 POST 요청 시 필터가 동작!
	        setFilterProcessesUrl("/api/public/login");
	    }
	
	
	// POST/login 요청이 왔을 때 인증을 시도하는 메소드
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		// request에서 json 타입의 [username/password]를 읽어서 Member 객체를 생성한다.
		
		log.info("\n"+"[진입] : [1][JWTAuthenticationFilter] POST 방식 로그인 시도 진입");
		
		ObjectMapper mapper = new ObjectMapper();
		
		
		try {
			MemberDTO.LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), MemberDTO.LoginRequest.class);
			// Security에게 자격 증명 요청에 필요한 객체 생성
			Authentication authToken = new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword());
			// 인증 진행 -> UserDetailsService의 loadUserByUsername에서 DB로부터 사용자 정보를 읽어온 뒤
			// 사용자 입력 정보와 비교한 뒤 자격 증명에 성공하면 Authenticaiton객체를 만들어서 리턴한다. 
			log.info("[성공] : [2][JWTAuthenticationFilter] request에서 json 타입의 [username/password]를 읽어서 Member 객체를 생성 \n");
			
			return authenticationManager.authenticate(authToken);
			
		}
		
		catch (Exception e) {
			// “자격 증명에 실패하였습니다.” 로그 출력
			log.info("[실패] : [2][JWTAuthenticationFilter] 사용자 인증 실패 \n");
			log.info(e.getMessage());
			// 자격 증명에 실패하면 응답코드 리턴
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		}
		
		return null;
	}
	
	
	// 인증이 성공했을 때 실행되는 후처리 메소드
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		
		// 자격 증명이 성공하면 loadUserByUsername에서 만든 객체가 authResult에 담겨져 있다.
		log.info("[성공] : [3][JWTAuthenticationFilter] 사용자 인증 성공");
		

		//User user = (User)authResult.getPrincipal(); // CustomUserDetails로 캐스팅
		CustomUserDetails user = (CustomUserDetails) authResult.getPrincipal();
		
		log.info("[유저] : {}", user.getUserId()); // user 객체를 콘솔에 출력해서 확인
		
		// username으로 JWT를 생성해서 Response Header - Authorization에 담아서 돌려준다.
		// 이것은 하나의 예시로서 필요에 따라 추가 정보를 담을 수 있다.
		log.info("[진행] : [4][JWTAuthenticationFilter] JWTAuthorizationFilter로 정보 이동");
		
		//Token에 Role 정보를 담기 위해 객체화 함.
		String role = user.getAuthorities()
              .stream()
              .findFirst()
              .map(auth -> auth.getAuthority().replace("ROLE_", "")) // "ROLE_MEMBER" → "MEMBER"
              .orElse("UNAUTH"); // 혹시 없으면 디폴트
		
		// FactoryCode 토큰에 넣기
		// 1. username 얻기
		String username = user.getUsername();

		// 2. MemberRepository로 Member 엔티티 가져오기
		Member member = memberRepo.findByUserId(username)
		    .orElseThrow(() -> new RuntimeException("Member not found"));

		// 3. factoryCode 꺼내기
		Long locationId = member.getAssetLocation().getLocationId();
		
		
		String token = JWT.create()
				.withExpiresAt(new Date(System.currentTimeMillis()+1000*60*100000))
				.withClaim("userId", user.getUsername()) //token에 넣을 정보
				.withClaim("role", role)
				.withClaim("location_id", locationId) // factoryCode
				.sign(Algorithm.HMAC256("edu.pnu.jwt"));
		
		response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		log.info("\n[진행] : [5][JWTAuthenticationFilter] Front에 Token Head로 전달 \n");
		
		response.setStatus(HttpStatus.OK.value());
	
	}

}
