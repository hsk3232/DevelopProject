package edu.pnu.dto;

import java.time.LocalDateTime;

import edu.pnu.domain.AssetLocation;
import edu.pnu.domain.Member;
import edu.pnu.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


public class MemberDTO {
	
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■ [ REQUEST DTO ] ■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //
	
	// [회원 가입 요청] : 회원 가입 요청을 위한 DTO
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class JoinRequest {
		private String userId;
		private String userName;
		private String password;
		private String email;
		private String phone;
		private Long locationId; // AssetLocation
		private Role role;
		
		public Member toEntity(String encryptedPassword) {
			AssetLocation ALocation = AssetLocation.builder()
					.locationId(locationId).build();
			
			return Member.builder()
					.userId(userId)
					.userName(userName)
					.password(encryptedPassword)
                    .email(email)
                    .phone(phone)
                    .role(role != null ? role : Role.ROLE_UNAUTH)
                    .assetLocation(ALocation)
                    .build();
		}
	}
	
	// [회원 정보 수정] : 자신의 회원 정보 수정을 요청하기 위한 DTO
	@Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UpdateInfoRequest {
        private String userName;
        private String email;
        private String phone;
    }
	
	
	
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■ [ RESPONSE  DTO ] ■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //

	// [회원 정보 응답] : 회원 정보 응답을 위한 DTO
	@Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class InfoResponse {
        private String userId;
        private String userName;
        private String email;
        private String status;
        private Long locationId;
        
        public static InfoResponse from(Member m) {
        	return InfoResponse.builder()
        			.userId(m.getUserId())
        			.userName(m.getUserName())
        			.email(m.getEmail())
        			.status(m.getStatus())
        			.locationId(m.getAssetLocation() != null ? m.getAssetLocation().getLocationId() : null)
        			.build();
        }
	}
	
	// [회원 설정 정보 응답] : 설정 페이지에 필요한 정보 응답
	@Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SettingResponse {
		private String userName;
		private String email;
		private String status;
		
		public static SettingResponse from(Member m) {
			return SettingResponse.builder()
					.userName(m.getUserName())
					.email(m.getEmail())
					.status(m.getStatus())
					.build();
		}
	}
	
	// [Admin 용] : 전체 사용자 목록 조회를 위한 상세 정보 응답
	@Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AdminDetailResponse {
		private Long memberId;
		private String userId;
		private String userName;
		private String email;
		private String phone;
		private Role role;
		private Long locationId;
		private String locationName;
		private String status;
		private LocalDateTime createdAt;
		
		public static AdminDetailResponse from(Member m) {
			return AdminDetailResponse.builder()
					.memberId(m.getMemberId())
					.userId(m.getUserId())
					.userName(m.getUserName())
					.email(m.getEmail())
					.phone(m.getPhone())
					.role(m.getRole())
					.locationId(m.getAssetLocation() != null ? m.getAssetLocation().getLocationId() : null)
					.locationName(m.getAssetLocation() != null? m.getAssetLocation().getScanLocation() : null)
					.status(m.getStatus())
					.createdAt(m.getCreatedAt())
					.build();
		}
	}
        
}
