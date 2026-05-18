package com.app.globalgates.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class FollowRecommendationRequestDTO {
    private Long memberId;
    private List<Long> excludeIds;
    private int topK;
}
