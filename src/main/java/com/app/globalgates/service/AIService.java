package com.app.globalgates.service;

import com.app.globalgates.dto.*;
import com.app.globalgates.repository.BlockDAO;
import com.app.globalgates.repository.MemberDAO;
import com.app.globalgates.repository.MemberProfileFileDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class AIService {
    private final FollowService followService;
    private final BlockDAO blockDAO;
    private final MemberDAO memberDAO;
    private final MemberProfileFileDAO memberProfileFileDAO;
    private final S3Service s3Service;

    public FollowRecommendationRequestDTO getFollowRecommendationRequest(Long memberId) {
        Set<Long> excludeIds = new LinkedHashSet<>();
        excludeIds.add(memberId);

        List<FollowDTO> followings = followService.getFollowings(memberId);
        for (FollowDTO follow : followings) {
            if (follow.getFollowingId() != null) {
                excludeIds.add(follow.getFollowingId());
            }
        }

        List<BlockDTO> blocks = blockDAO.findAllByBlockerId(memberId);
        for (BlockDTO block : blocks) {
            if (block.getBlockedId() != null) {
                excludeIds.add(block.getBlockedId());
            }
        }

        FollowRecommendationRequestDTO requestDTO = new FollowRecommendationRequestDTO();
        requestDTO.setMemberId(memberId);
        requestDTO.setExcludeIds(new ArrayList<>(excludeIds));
        requestDTO.setTopK(3);
        return requestDTO;
    }

    public List<FriendsDTO> getRecommendMembers(FollowRecommendationResponseDTO response) {
        List<FriendsDTO> friends = new ArrayList<>();

        if (response == null || response.getRecommendations() == null) {
            return friends;
        }

        for (FollowRecommendationItemDTO item : response.getRecommendations()) {
            Optional<MemberDTO> memberOptional = memberDAO.findByMemberId(item.getMemberId());
            if (memberOptional.isEmpty()) {
                continue;
            }

            MemberDTO member = memberOptional.get();

            FriendsDTO friend = new FriendsDTO();
            friend.setId(member.getId());
            friend.setMemberName(member.getMemberName());
            friend.setMemberNickname(member.getMemberNickname());
            friend.setMemberHandle(member.getMemberHandle());
            friend.setMemberBio(member.getMemberBio());
            friend.setIsFollowing(false);

            if (member.getMemberRole() != null) {
                friend.setMemberRole(member.getMemberRole().name());
            }

            MemberProfileFileDTO profileFile = memberProfileFileDAO.findByMemberId(member.getId());
            if (profileFile != null && profileFile.getFileName() != null) {
                friend.setMemberProfileFileName(profileFile.getFileName());
            }

            friends.add(friend);
        }

        for (FriendsDTO friend : friends) {
            if (friend.getMemberProfileFileName() != null
                    && !friend.getMemberProfileFileName().startsWith("http")
                    && !friend.getMemberProfileFileName().startsWith("/uploads/")) {
                try {
                    friend.setMemberProfileFileName(
                            s3Service.getPresignedUrl(friend.getMemberProfileFileName(), Duration.ofMinutes(10))
                    );
                } catch (IOException e) {
                    log.error("프로필 Presigned URL 생성 실패: {}", friend.getMemberProfileFileName(), e);
                    friend.setMemberProfileFileName(null);
                }
            }
        }

        return friends;
    }
}

