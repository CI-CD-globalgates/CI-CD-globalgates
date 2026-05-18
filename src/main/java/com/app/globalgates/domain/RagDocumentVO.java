package com.app.globalgates.domain;

import com.app.globalgates.common.enumeration.RagDocumentStatus;
import com.app.globalgates.common.enumeration.RagProcessStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class RagDocumentVO {
    private Long id;
    private Long fileId;
    private String documentName;
    private RagDocumentStatus documentStatus;
    private RagProcessStatus ragStatus;
    private Long uploadedBy;
    private String lastError;
    private String createdAt;
    private String updatedAt;
}
