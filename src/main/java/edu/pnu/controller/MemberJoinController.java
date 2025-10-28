package edu.pnu.controller;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.dto.MemberDTO;
import edu.pnu.exception.UnauthorizedException;
import edu.pnu.service.member.MemberJoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class MemberJoinController {

    private final MemberJoinService memberJoinService;


    @PostMapping("/join")
    public void postJoin(@RequestBody MemberDTO.JoinRequest dto) {
        try {
            log.info("[진입] : [MemberJoinController] 회원가입 진입");

            memberJoinService.postJoin(dto);
            log.info("[성공] : [MemberJoinController] 회원가입 성공");
        } catch (Exception e) {
            log.info("[실패] : [MemberJoinController] 회원가입 실패 " + e.getMessage());

        }
    }

    @PostMapping("/join/idsearch")
    public boolean postIdSearch(@RequestBody MemberDTO.LoginRequest dto) {
        boolean result = memberJoinService.postIdSearch(dto.getUserId());
        if (result) {
            log.info("[경고/오류] : [JoinController][이미 사용 중인 아이디입니다.]" + "\n");
            return true;
        }
        log.info("[성공] : [JoinController] 사용할 수 있는 아이디 검색^^ \n");
        return false;
    }



    @GetMapping("/unauth")
    public ResponseEntity<?> getUnauthPage(@AuthenticationPrincipal CustomUserDetails user) {
        log.info("[진입] : [LoginController] 미승인 가입자 페이지 연결 진입");

        try {
            memberJoinService.checkUnauthStatus(user.getUserId());
        } catch (UnauthorizedException e) {
            log.warn("[인가 실패] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("[오류] " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }

        // 인가된 경우 (이 메서드는 인가 안된 사용자용이므로, 여기까지 온다면 허용)
        return ResponseEntity.ok("접근 권한이 있습니다.");
    }
}
