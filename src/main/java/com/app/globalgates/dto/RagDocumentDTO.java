package com.app.globalgates.dto;

import com.app.globalgates.common.enumeration.RagDocumentStatus;
import com.app.globalgates.common.enumeration.RagProcessStatus;
import com.app.globalgates.domain.RagDocumentVO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
public class RagDocumentDTO {
    private Long id;
    private Long fileId;
    private String documentName;
    private RagDocumentStatus documentStatus;
    private RagProcessStatus ragStatus;
    private Long uploadedBy;
    private String lastError;
    private String createdAt;
    private String updatedAt;

    public RagDocumentVO toVO() {
        return RagDocumentVO.builder()
                .id(id)
                .fileId(fileId)
                .documentName(documentName)
                .documentStatus(documentStatus)
                .ragStatus(ragStatus)
                .uploadedBy(uploadedBy)
                .lastError(lastError)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
