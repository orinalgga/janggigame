package com.janggi.server.chat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 대국 채팅의 답을 Claude에게 받는다. API 키가 없으면 꺼져 있고, 실패하면 빈 값을 돌려
 * 정해 둔 말로 대신 답하게 한다.
 */
@Component
public class ClaudeChat {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChat.class);

    private final AnthropicClient client;
    private final String model;

    public ClaudeChat(@Value("${janggi.ai-chat.api-key:}") String apiKey,
            @Value("${janggi.ai-chat.model:claude-opus-5-5}") String model) {
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(1)
                .build();
        this.model = model;
    }

    public boolean enabled() {
        return client != null;
    }

    /** turns는 오래된 것부터, 첫 말은 사람의 말이어야 한다. */
    Optional<String> reply(String system, List<Turn> turns) {
        if (client == null || turns.isEmpty()) {
            return Optional.empty();
        }
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2000L)
                .system(system)
                // 짧은 잡담이라 깊이 생각할 필요가 없다.
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build());
        for (Turn t : turns) {
            if (t.ai()) {
                params.addAssistantMessage(t.text());
            } else {
                params.addUserMessage(t.text());
            }
        }
        try {
            Message response = client.messages().create(params.build());
            String text = response.content().stream()
                    .flatMap(b -> b.text().stream())
                    .map(b -> b.text())
                    .collect(Collectors.joining())
                    .strip();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (RuntimeException e) {
            log.warn("Claude 채팅 응답 실패: {}", e.toString());
            return Optional.empty();
        }
    }

    /** 대화 한 차례. 같은 쪽 말이 이어지면 한 차례로 합쳐 넘긴다. */
    record Turn(boolean ai, String text) {
    }
}
