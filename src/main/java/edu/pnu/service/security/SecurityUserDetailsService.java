package edu.pnu.service.security;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Member;
import edu.pnu.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;


    //	Spring Security가 인증을 위해 userId로 사용자를 조회할 때 호출하는 메서드
    @Override
    @Transactional(readOnly = true) // 사용자 정보 조회 시 Lazy Loading을 지원하기 위해 트랜잭션 설정
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("[진입] : [SecurityUserDetailsService] 사용자 정보 조회를 시작합니다. UserID: {}", userId);

        // 1. MemberRepository를 사용하여 DB에서 Member 엔티티를 조회
        Member member = memberRepository.findByUserId(userId).orElseThrow(() -> {
            log.warn("[실패] : [SecurityUserDetailsService] 사용자 [{}] 정보를 찾을 수 없습니다.", userId);
            return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        });

        log.info("[성공] : [SecurityUserDetailsService] 사용자 [{}] 정보를 성공적으로 조회했습니다.", userId);

        // 2. 조회된 Member 엔티티를 CustomUserDetails 객체로 변환하여 반환
        // CustomUserDetails.from() 팩토리 메서드를 사용하여 생성 로직을 캡슐화
        return CustomUserDetails.from(member);
    }
}
