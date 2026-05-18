package com.app.globalgates.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class FollowRecommendationItemDTO {
    private Long memberId;
    private String categoryText;
    private double score;
    private int rankPosition;
    private String candidateSource;
}
