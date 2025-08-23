package edu.pnu.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import edu.pnu.domain.Member;
import edu.pnu.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class UserManagementDTO {

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■ [ REQUEST DTO ] ■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //

	// [Admin 용] : 사용자의 상태 변경을 요청
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class UpdateStatusRequest {
		private String userId;
		private String status;
	}

	// [Admin 용] : 사용자의 소속 공장(위치) 변경을 요청
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class UpdateFactoryRequest {
		private String userId;
		private Long locationId;
	}

	
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■ [ RESPONSE DTO ] ■■■■■■■■■■■■ //
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ //

	// [회원 정보 응답] : 회원 정보 응답을 위한 DTO
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class UserInfo {
		private Long memberId;
		private String userId;
		private String userName;
		private Role role;
		private Long loacationId;
		private String locationName;
		private String status;
		private LocalDateTime createdAt;
		
		public static UserInfo from(Member m) {
			return UserInfo.builder()
					.memberId(m.getMemberId())
					.userId(m.getUserId())
					.userName(m.getUserName())
					.role(m.getRole())
					.loacationId(m.getAssetLocation() != null ? m.getAssetLocation().getLocationId() : null)
					.locationName(m.getAssetLocation() != null ? m.getAssetLocation().getScanLocation() : null)
					.status(m.getStatus())
					.createdAt(m.getCreatedAt())
					.build();
		}
	}
	
	// [API 응답의 최상위 객체] : 기존 ResponseEntity.ok(Map.of("users", users)) 부분을 대체
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class UserListResponse {
		private List<UserInfo> users;
		
		public static UserListResponse from(List<Member> ml) {
			List<UserInfo> userInfos = ml.stream()
					.map(UserInfo::from)
					.collect(Collectors.toList());
			return new UserListResponse(userInfos);
		}
	}
	
	
}
