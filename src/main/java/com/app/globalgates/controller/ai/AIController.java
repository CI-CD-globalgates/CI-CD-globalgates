package com.app.globalgates.controller.ai;

import com.app.globalgates.dto.*;
import com.app.globalgates.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai/**")
public class AIController {
    private final WebClient.Builder webClientBuilder;
    private final AIService aiService;

    @Value("${ai.fastapi.base-url}")
    private String fastApiBaseUrl;

    private WebClient fastApiClient() {
        return webClientBuilder
                .clone()
                .baseUrl(fastApiBaseUrl)
                .build();
    }

    @PostMapping("/category/predict")
    @ResponseBody
    public Mono<ProductCategoryRecommendationResponseDTO> getProductCategoryRecommendation(@RequestBody ProductCategoryRecommendationRequestDTO productCategoryRecommendationRequestDTO) {
        log.info("body: {}", productCategoryRecommendationRequestDTO);

        return fastApiClient().post()
                .uri("/api/ai/category/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productCategoryRecommendationRequestDTO)
                .retrieve()
                .bodyToMono(ProductCategoryRecommendationResponseDTO.class);
    }

    @GetMapping("/follow/recommend/{memberId}")
    @ResponseBody
    public Mono<List<FriendsDTO>> getFollowRecommendation(@PathVariable Long memberId) {
        FollowRecommendationRequestDTO requestDTO = aiService.getFollowRecommendationRequest(memberId);
        log.info("body: {}", requestDTO);
        return fastApiClient().post()
                .uri("/api/ai/follow/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(FollowRecommendationResponseDTO.class)
                .map(aiService::getRecommendMembers);
    }

    @PostMapping("/chat/query")
    @ResponseBody
    public Mono<ChatbotQueryResponseDTO> queryChatbot(@RequestBody ChatbotQueryRequestDTO requestDTO) {
        log.info("chatbot body: {}", requestDTO);

        return fastApiClient().post()
                .uri("/api/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(ChatbotQueryResponseDTO.class);
    }
}
