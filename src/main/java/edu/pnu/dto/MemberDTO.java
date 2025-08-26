package edu.pnu.dto;

import edu.pnu.domain.AssetLocation;
import edu.pnu.domain.Member;
import edu.pnu.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public abstract class MemberDTO {


//	■■■■■■■■■■■■ [ REQUEST DTO ] ■■■■■■■■■■■■

	// [회원 가입 요청] : 회원 가입 요청을 위한 DTO
	@Getter
	@Setter
	@ToString(exclude = "password")
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class JoinRequest {
		@NotBlank
		private String userId;
		@NotBlank
		private String userName;
		@NotBlank
		private String password;
		@Email
		private String email;
		private String phone;
		@Positive
		private Long locationId; // AssetLocation
		private Role role;

		public Member toEntity(String encryptedPassword) {
			AssetLocation ALocation = AssetLocation.builder().locationId(locationId).build();

			return Member.builder()
					.userId(userId)
					.userName(userName)
					.password(encryptedPassword)
					.email(email)
					.phone(phone)
					.role(Role.ROLE_UNAUTH)
					.assetLocation(ALocation)
					.build();
		}
	}
	
	// [ID 중복 확인] : 아이디 중복 확인 요청을 위한 DTO 
	// 기존 LoginDTO의 idsearch 역할
    @Getter 
    @Setter
    public static class IdSearchRequest {
    	@NotBlank
    	private String userId;
    }

	// [로그인] : 회원 로그인 요청을 위한 DTO
	// 기존 LoginDTO
	@Getter
	@Setter
	@Builder
	@ToString(exclude = {"password"})
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LoginRequest { // 기존 LoginDTO 역할
		@NotBlank
		private String userId;
		@NotBlank
		private String password;
	}

	// [회원 정보 수정] : 자신의 회원 정보 수정을 요청하기 위한 DTO
	// 기존 MemberUpdateDTO
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
	
	// [비밀번호 수정] : 자신의 비밀번호 수정을 요청하기 위한 DTO
	// 기존 PasswordChangeRequestDTO
	@Getter
	@Setter
	@Builder
	@ToString(exclude = {"currentPassword","newPassword"})
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ChangePasswordRequest {
		@NotBlank
		private String currentPassword;
		@NotBlank
		private String newPassword;
	}
	
	


//	 ■■■■■■■■■■■■ [ RESPONSE DTO ] ■■■■■■■■■■■■ 

	
	// [회원 정보 응답] : 회원 정보 응답을 위한 DTO
	// 기존 MemberResponseDTO
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
					.locationId(m.getAssetLocation() != null ? m.getAssetLocation().getLocationId() : null).build();
		}
	}

	// [회원 설정 정보 응답] : 설정 페이지에 필요한 정보 응답
	// 기존 MemberSettingDTO
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

}
