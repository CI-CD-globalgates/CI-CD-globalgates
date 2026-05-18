package com.app.globalgates.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ProductCategoryRecommendationResponseDTO {
   private List<ProductCategoryPredictionItemDTO> predictions;
}
