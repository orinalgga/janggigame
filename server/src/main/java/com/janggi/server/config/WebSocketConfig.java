package com.janggi.server.config;

import com.janggi.server.auth.JwtService;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 온라인 대전용 STOMP. 대국 화면은 /topic/games/{id}를, 매칭을 기다리는 회원은 /user/queue/match를 구독한다.
 * 수는 REST로 두고, 서버는 바뀐 국면을 여기로 알리기만 한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwt;

    public WebSocketConfig(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 하트비트: 휴대폰이 신호 없이 끊겨도(터널·비행기 모드) 서버가 연결이 죽은 걸 알아채게 한다.
        // 브라우저는 뒤로 간 탭의 타이머를 1분까지 늦추므로, 서버가 기다리는 시간(25초의 3배)을 그보다 길게 둔다.
        ThreadPoolTaskScheduler heartbeat = new ThreadPoolTaskScheduler();
        heartbeat.setPoolSize(1);
        heartbeat.setThreadNamePrefix("ws-heartbeat-");
        heartbeat.initialize();
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {25_000, 25_000})
                .setTaskScheduler(heartbeat);
        registry.setApplicationDestinationPrefixes("/app");
    }

    /** CONNECT 헤더의 Authorization: Bearer 토큰으로 회원을 정한다. 회원 id가 /user 목적지의 이름이 된다. */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        jwt.verify(header.substring(7)).ifPresent(userId -> accessor.setUser(
                                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
                    }
                }
                return message;
            }
        });
    }
}
