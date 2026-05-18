package com.app.globalgates.dto;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ProductCategoryPredictionItemDTO {
    private String categoryName;
    private double score;
}
