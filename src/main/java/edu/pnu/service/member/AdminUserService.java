package edu.pnu.service.member;

import edu.pnu.domain.AssetLocation;
import edu.pnu.domain.Member;
import edu.pnu.dto.UserManagementDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.repository.AssetLocationRepository;
import edu.pnu.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final MemberRepository memberRepo;
    private final AssetLocationRepository assetLocationRepo;


    // 사용자 목록 조회
    @Transactional
    public List<UserManagementDTO.UserInfo> getUserList() {
        List<Member> members = memberRepo.findAll();
        return members.stream()
                .map(this::toUserResponseDTO)
                .collect(Collectors.toList());
    }

    private UserManagementDTO.UserInfo toUserResponseDTO(Member m) {

        return UserManagementDTO.UserInfo.builder()
                .role(m.getRole()) // Role enum to String
                .locationId(m.getAssetLocation() != null ? m.getAssetLocation().getLocationId() : null)
                .userId(m.getUserId())
                .userName(m.getUserName())
                .status(m.getStatus()) // String or Enum; 맞춰서 사용하세요.
                .createdAt(m.getCreatedAt())
                .build();
    }

    // 상태 변경
    @Transactional
    public void updateUserStatus(UserManagementDTO.UpdateStatusRequest dto) {
        Member m = memberRepo.findByUserId(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("[오류] 회원 정보가 없습니다: "));
        if (m == null) {
            throw new NoDataFoundException("사용자를 찾을 수 없습니다: " + dto.getUserId());
        }

        m.setStatus(dto.getStatus());
        memberRepo.save(m);
    }

    @Transactional
    public void updateUserFactory(UserManagementDTO.UpdateFactoryRequest dto) {
        Member member = memberRepo.findByUserId(dto.getUserId())
                .orElseThrow(() -> new NoDataFoundException("[오류] 회원을 찾을 수 없습니다. userId=" + dto.getUserId()));

        AssetLocation location = assetLocationRepo.findById(dto.getLocationId())
                .orElseThrow(() -> new NoDataFoundException("[오류] 위치를 찾을 수 없습니다. locationId=" + dto.getLocationId()));

        member.setAssetLocation(location);
        memberRepo.save(member);
    }

}
