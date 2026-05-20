package com.app.globalgates.service.chat.event;

import com.app.globalgates.dto.chat.ChatMessageDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

// 메시지 INSERT가 트랜잭션 커밋된 직후에 발행되는 이벤트.
// 트랜잭션 안에서 Rabbit publish/Notification/WebSocket을 호출하면
// 후속 로직의 예외로 메시지가 롤백되어도 외부 효과는 남아 새로고침 시
// 사라지는 메시지가 발생한다. 모든 외부 효과는 이 이벤트의 AFTER_COMMIT
// 리스너에서 처리한다.
@Getter
@RequiredArgsConstructor
public class ChatMessageSentEvent {
    private final ChatMessageDTO message;
    private final List<Long> restoredMemberIds;
}
