package com.app.globalgates.dto;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ProductCategoryRecommendationRequestDTO {
    private String postTitle;
    private String postContent;
    private String postTag;
}
