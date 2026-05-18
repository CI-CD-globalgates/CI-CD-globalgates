package com.app.globalgates.dto;

import com.app.globalgates.common.enumeration.RagProcessStatus;
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
public class RagIngestResponseDTO {
    private Long documentId;
    private String documentName;
    private RagProcessStatus ragStatus;
    private String message;
}
