package com.janggi.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.core.Side;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import com.janggi.server.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"janggi.online.base-seconds=1", "janggi.online.increment-seconds=0"})
class OnlineTimeoutTest {

    @Autowired
    GameService games;

    @Autowired
    UserService users;

    @Test
    void sideToMoveLosesOnTimeEvenIfNobodyMoves() throws Exception {
        long cho = users.loginWith("google", UUID.randomUUID().toString(), null).getId();
        long han = users.loginWith("google", UUID.randomUUID().toString(), null).getId();
        String id = games.createOnline(cho, Formation.MA_SANG_MA_SANG, han, Formation.MA_SANG_MA_SANG).id();

        GameDto game = games.get(id);
        for (int i = 0; i < 40 && game.result() == GameResult.ONGOING; i++) {
            Thread.sleep(100);
            game = games.get(id);
        }
        assertEquals(GameResult.HAN_WIN, game.result());
        assertEquals("TIMEOUT", game.endReason());
        assertEquals(0, game.clock().choMs());
        assertEquals(null, game.clock().running());
        assertEquals(Side.CHO, game.sideToMove());
    }
}
