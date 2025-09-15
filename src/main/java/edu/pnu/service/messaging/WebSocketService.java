package edu.pnu.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate; // Spring이 제공하는 메시지 발송 유틸 클래스.

    // 특정 사용자(userId)에게 메시지 발송
    public void sendMessage(String userId, String message) {
        // convertAndSend(destination, message) → 해당 destination(구독 주소, 채널)에 메시지 전달.
        // (예: /topic/notify/홍길동ID, /topic/notify/2 등)
        messagingTemplate.convertAndSend("/notify/" + userId, message);
    }
}
