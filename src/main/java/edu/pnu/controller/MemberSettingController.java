package edu.pnu.controller;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.dto.MemberDTO;
import edu.pnu.service.member.MemberSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class MemberSettingController {
    private final MemberSettingService memberSettingService;

    @GetMapping("/setting/user")
    public ResponseEntity<MemberDTO.SettingResponse> getSettingUser(
            @AuthenticationPrincipal CustomUserDetails user) {
        log.info("[진입] : [LoginController] 가입자 페이지 연결 진입");

        MemberDTO.SettingResponse m = memberSettingService.getSettingUser(user.getUserId());
        return ResponseEntity.ok(m);

    }

    @PostMapping("/setting/password")
    public ResponseEntity<String> getSettingPasswowrd(
            @AuthenticationPrincipal CustomUserDetails user, @RequestBody MemberDTO.ChangePasswordRequest req) {
        log.info("[진입] : [LoginController] 페스워드 페이지 연결 진입");

        memberSettingService.getSettingPasswowrd(user.getUserId(), req.getCurrentPassword(), req.getNewPassword());
        log.info("[차단] : [LoginController] 승인된 사용자가 요청함");
        return ResponseEntity.status(HttpStatus.CONFLICT).body("변경 성공");

    }


    @PatchMapping("/setting/info")
    public ResponseEntity<MemberDTO.InfoResponse> updateMember(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody MemberDTO.InfoResponse dto) {
        log.info("[진입] : [LoginController] 개인정보 수정 연결 진입");
        String userId = user.getUserId();

        MemberDTO.InfoResponse updated = memberSettingService.updateMember(userId, dto);
        log.info("[진입] : [LoginController] 개인정보 완료");
        System.out.println(updated);
        return ResponseEntity.ok(updated);
    }
}
