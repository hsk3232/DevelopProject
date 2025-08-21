package edu.pnu.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long memberId; //auto-Increment
	
	@Column(name = "user_id", unique = true) //Join시에는 name 써줘야함, FK 참조를 위해서는 unique 키임을 명시해야함.
	private String userId;
	
	private String password; //비밀번호
	
	private String userName; //가입자 이름
	
	@Builder.Default
	@Enumerated(EnumType.STRING)
	private Role role = Role.ROLE_UNAUTH; // 허용 범위
	
	 // factoryCode 대신 Location 연관관계로 대체!
    @ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = 현재 테이블에 생길 새로운 칼럼명, referenced = 참조할 테이블의 PK)
    @JoinColumn(name = "location_id")
    private AssetLocation assetLocation;
	
	private String email;
	private String phone;
	
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    private String status;
}
