package com.app.globalgates.mapper;

import com.app.globalgates.common.enumeration.RagProcessStatus;
import com.app.globalgates.dto.RagDocumentDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RagDocumentMapper {
    public void insert(RagDocumentDTO ragDocumentDTO);

    public void updateRagStatus(
            @Param("id") Long id,
            @Param("ragStatus") RagProcessStatus ragStatus,
            @Param("lastError") String lastError
    );
}
