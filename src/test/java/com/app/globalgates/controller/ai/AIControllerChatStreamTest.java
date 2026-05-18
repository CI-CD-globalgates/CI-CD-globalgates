package com.app.globalgates.controller.ai;

import com.app.globalgates.dto.ChatbotQueryRequestDTO;
import com.app.globalgates.dto.ChatbotQueryResponseDTO;
import com.app.globalgates.service.AIService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class AIControllerChatStreamTest {

    @Mock
    private AIService aiService;

    @InjectMocks
    private AIController aiController;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void queryChatbot_returnsStructuredResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat/query", exchange -> writeJsonResponse(
                exchange,
                "{\"answer\":\"안녕\",\"cached\":false,\"sources\":[]}"
        ));
        server.start();

        ReflectionTestUtils.setField(
                aiController,
                "webClient",
                WebClient.create("http://127.0.0.1:" + server.getAddress().getPort())
        );

        ChatbotQueryRequestDTO requestDTO = new ChatbotQueryRequestDTO();
        requestDTO.setQuestion("안녕?");

        Mono<ChatbotQueryResponseDTO> responseMono = aiController.queryChatbot(requestDTO);
        ChatbotQueryResponseDTO responseDTO = responseMono.block();

        assertEquals("안녕", responseDTO.getAnswer());
        assertFalse(responseDTO.getCached());
        assertEquals(Collections.emptyList(), responseDTO.getSources());
    }

    private void writeJsonResponse(HttpExchange exchange, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, payload.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }
}
