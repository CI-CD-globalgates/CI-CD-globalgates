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
public class ChatbotQueryResponseDTO {
    private String answer;
    private Boolean cached;
    private List<Object> sources;
}
