package edu.pnu.service.member;

import edu.pnu.domain.Member;
import edu.pnu.dto.MemberDTO;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.exception.UnauthorizedException;
import edu.pnu.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberJoinService {
    private final MemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    // 1. 회원가입
    public void postJoin(MemberDTO.JoinRequest dto) {

        // Global Exception : 아이디 중복 체크 1번 더
        if (memberRepo.existsByUserId(dto.getUserId())) {
            throw new BadRequestException("[오류] : [MemberJoinController] 동일 id 검색됨" + dto.getUserId());
        }

        // 회원가입 정보 저장 및 암호 해시
        Member m = dto.toEntity();
        m.setPassword(passwordEncoder.encode(dto.getPassword()));
        m.setStatus("pending");
        memberRepo.save(m);
    }


    // 2. id 중복 여부 확인 -> api 따로 있기 때문에 필요함.
    public boolean postIdSearch(String userId) {
        boolean exist = memberRepo.existsByUserId(userId);
        System.out.println(exist);
        if (exist) {
            System.out.println("[오류] : [MemberJoinController] 동일 id 검색됨");
        }
        return exist;
    }


    public void checkUnauthStatus(String userId) {
        Member m = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("[오류] 회원 정보가 없습니다: "));

        String status = m.getStatus();
        if (status == null) {
            throw new NoDataFoundException("[오류] 상태 정보가 없습니다: " + userId);
        }

        // 상태가 승인 대기, 거절, 삭제 등 인가 안된 상태면 예외 처리
        if ("pending".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status) || "del".equalsIgnoreCase(status)) {
            throw new UnauthorizedException("인가되지 않은 사용자입니다: " + userId);
        }

    }
}
