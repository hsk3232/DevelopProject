package edu.pnu.service.member;

import edu.pnu.domain.Member;
import edu.pnu.dto.MemberDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSettingService {
    private final MemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberDTO.SettingResponse getSettingUser(String userId) {

        Optional<Member> member = memberRepo.findByUserId(userId);

        Member m = member.orElse(null);

        if (m == null) {
            throw new NoDataFoundException("[오류] : 찾는 사용자가 없습니다.");
        }
        return MemberDTO.SettingResponse.from(m);

    }

    @Transactional
    public void getSettingPasswowrd(String userId, String currentPassword, String newPassword) {
        Optional<Member> member = memberRepo.findByUserId(userId);

        Member m = member.orElseThrow(() -> new IllegalArgumentException("[오류] 회원 정보가 없습니다: " + userId));

        if (!passwordEncoder.matches(currentPassword, m.getPassword())) {
            throw new IllegalArgumentException("[오류] 현재 비밀번호가 일치하지 않습니다.");
        }

        m.setPassword(passwordEncoder.encode(newPassword));
        memberRepo.save(m);

    }

    @Transactional
    public MemberDTO.InfoResponse updateMember(String userId, MemberDTO.InfoResponse dto) {
        Member m = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new NoDataFoundException("[오류] 회원 정보가 없습니다: " + userId));

        log.info("[Before] userName = {}", m.getUserName());
        if (dto.getUserName() != null) {
            m.setUserName(dto.getUserName());
            log.info("[After] userName = {}", m.getUserName());
        }
        if (dto.getEmail() != null) {
            m.setEmail(dto.getEmail());
        }
        if (dto.getStatus() != null) {
            m.setStatus(dto.getStatus());
        }

        // 명시적 save 호출 (선택적이나 안정성을 위해 권장)
        memberRepo.save(m);
        memberRepo.flush(); // 명시적 flush 호출 시도

        return MemberDTO.InfoResponse.from(m);
    }

}
