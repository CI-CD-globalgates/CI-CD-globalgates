package com.app.globalgates.service;

import com.app.globalgates.dto.chat.ChatMessageDTO;
import com.app.globalgates.dto.chat.ChatRoomDTO;
import com.app.globalgates.dto.FileDTO;
import com.app.globalgates.repository.chat.ChatMessageDAO;
import com.app.globalgates.repository.chat.ChatRoomDAO;
import com.app.globalgates.service.chat.ChatFileService;
import com.app.globalgates.service.chat.event.ChatMessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerService {
    private final ChatMessageDAO chatMessageDAO;
    private final ChatRoomDAO chatRoomDAO;
    private final ChatFileService chatFileService;
    private final BlockService blockService;
    private final ApplicationEventPublisher eventPublisher;

    private void validateNotBlocked(Long conversationId, Long senderId) {
        Optional<ChatRoomDTO> partner = chatRoomDAO.findPartnerByConversation(conversationId, senderId);
        if (partner.isEmpty()) {
            throw new IllegalStateException("대화방을 찾을 수 없습니다.");
        }
        Long partnerId = partner.get().getInvitedId();
        if (blockService.isBlockedEither(senderId, partnerId)) {
            throw new IllegalStateException("차단된 사용자에게 메시지를 보낼 수 없습니다.");
        }
    }

    // 메시지 INSERT + 복원만 트랜잭션 내에서 수행한다.
    // Rabbit publish / Notification / WebSocket 복원 통지는 트랜잭션 커밋 후
    // ChatMessageEventListener 에서 처리된다 (AFTER_COMMIT).
    @Transactional
    public ChatMessageDTO sendMessage(ChatMessageDTO chatMessageDTO) {
        validateNotBlocked(chatMessageDTO.getConversationId(), chatMessageDTO.getSenderId());
        ChatMessageDTO saved = chatMessageDAO.save(chatMessageDTO);
        log.info("메시지 DB 저장 완료 - id: {}", saved.getId());

        saved.setSenderName(chatMessageDTO.getSenderName());

        List<Long> deletedMemberIds = chatRoomDAO.findDeletedMemberIds(saved.getConversationId());
        chatRoomDAO.restoreAllMembers(saved.getConversationId());

        eventPublisher.publishEvent(new ChatMessageSentEvent(saved, deletedMemberIds));

        return saved;
    }

    @Transactional
    public ChatMessageDTO sendMessageWithFile(ChatMessageDTO chatMessageDTO, MultipartFile file) throws IOException {
        validateNotBlocked(chatMessageDTO.getConversationId(), chatMessageDTO.getSenderId());
        ChatMessageDTO saved = chatMessageDAO.save(chatMessageDTO);
        log.info("메시지 DB 저장 완료 - id: {}", saved.getId());

        saved.setSenderName(chatMessageDTO.getSenderName());

        FileDTO fileDTO = chatFileService.uploadAndLink(file, saved.getId());
        saved.setFileId(fileDTO.getId());
        saved.setFileOriginalName(fileDTO.getOriginalName());
        saved.setFilePath(fileDTO.getFilePath());
        saved.setFileSize(fileDTO.getFileSize());
        saved.setFileContentType(fileDTO.getContentType().getValue());

        List<Long> deletedMemberIds = chatRoomDAO.findDeletedMemberIds(saved.getConversationId());
        chatRoomDAO.restoreAllMembers(saved.getConversationId());

        eventPublisher.publishEvent(new ChatMessageSentEvent(saved, deletedMemberIds));

        return saved;
    }
}
