package com.app.globalgates.service.chat.event;

import com.app.globalgates.common.enumeration.NotificationType;
import com.app.globalgates.config.RabbitmqConfig;
import com.app.globalgates.dto.NotificationDTO;
import com.app.globalgates.dto.chat.ChatMessageDTO;
import com.app.globalgates.dto.chat.ChatRoomDTO;
import com.app.globalgates.repository.chat.ChatRoomDAO;
import com.app.globalgates.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

// 메시지 INSERT 트랜잭션이 커밋된 후에만 Rabbit publish / 알림 생성 /
// WebSocket 복원 통지를 수행한다. 트랜잭션 안에서 이들을 호출하면
// 후속 예외로 INSERT가 롤백되어도 외부 효과가 남아 클라이언트 화면에는
// 메시지가 보이지만 새로고침 시 사라지는 영속화 불일치가 발생한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageEventListener {
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final ChatRoomDAO chatRoomDAO;

    // REQUIRES_NEW: 원본 트랜잭션은 이미 커밋된 상태이므로 알림 INSERT를 위해
    // 새 트랜잭션을 시작한다. 여기서 예외가 발생해도 메시지 INSERT에는 영향 없음.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onChatMessageSent(ChatMessageSentEvent event) {
        ChatMessageDTO saved = event.getMessage();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.CHAT_EXCHANGE,
                    RabbitmqConfig.CHAT_ROUTING_KEY,
                    saved
            );
            log.info("RabbitMQ 발행 완료 (AFTER_COMMIT) - conversationId: {}", saved.getConversationId());
        } catch (Exception e) {
            log.error("RabbitMQ 발행 실패 - messageId: {}", saved.getId(), e);
        }

        notifyRestoredMembers(event.getRestoredMemberIds(), saved.getConversationId());

        try {
            sendMessageNotification(saved);
        } catch (Exception e) {
            log.error("메시지 알림 생성 실패 - messageId: {}", saved.getId(), e);
        }
    }

    private void notifyRestoredMembers(List<Long> memberIds, Long conversationId) {
        if (memberIds == null || memberIds.isEmpty()) return;
        Map<String, Long> payload = Map.of("conversationId", conversationId);
        for (Long memberId : memberIds) {
            messagingTemplate.convertAndSend("/topic/user." + memberId + ".restore", payload);
            log.info("방 복원 알림 전송 - memberId: {}, conversationId: {}", memberId, conversationId);
        }
    }

    private void sendMessageNotification(ChatMessageDTO saved) {
        chatRoomDAO.findPartnerByConversation(saved.getConversationId(), saved.getSenderId())
                .map(ChatRoomDTO::getInvitedId)
                .ifPresent(partnerId -> {
                    NotificationDTO noti = new NotificationDTO();
                    noti.setRecipientId(partnerId);
                    noti.setSenderId(saved.getSenderId());
                    noti.setNotificationType(NotificationType.MESSAGE);
                    noti.setTitle("메시지");
                    noti.setContent("새 메시지가 도착했습니다.");
                    noti.setTargetId(saved.getConversationId());
                    noti.setTargetType("conversation");
                    notificationService.createNotification(noti);
                });
    }
}
