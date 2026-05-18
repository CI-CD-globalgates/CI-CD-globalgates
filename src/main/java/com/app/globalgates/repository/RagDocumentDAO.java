package com.app.globalgates.repository;

import com.app.globalgates.common.enumeration.RagProcessStatus;
import com.app.globalgates.dto.RagDocumentDTO;
import com.app.globalgates.mapper.RagDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RagDocumentDAO {
    private final RagDocumentMapper ragDocumentMapper;

    // RAG 문서 메타는 tbl_file 과 별개로 상태 추적용 row를 하나 더 둔다.
    public void save(RagDocumentDTO ragDocumentDTO) {
        ragDocumentMapper.insert(ragDocumentDTO);
    }

    // 적재 상태와 마지막 에러를 같이 남겨야 운영에서 실패 원인을 바로 볼 수 있다.
    public void updateRagStatus(Long id, RagProcessStatus ragStatus, String lastError) {
        ragDocumentMapper.updateRagStatus(id, ragStatus, lastError);
    }
}
