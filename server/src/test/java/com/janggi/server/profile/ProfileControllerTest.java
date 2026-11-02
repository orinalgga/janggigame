package com.janggi.server.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.server.record.GameRecorder;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    GameRecorder recorder;

    @Test
    void countsOnlineRecordFromThatMembersSide() throws Exception {
        User a = users.loginWith("google", "profile-a", null);
        User b = users.loginWith("google", "profile-b", null);
        play(a, b, GameResult.CHO_WIN); // a 승
        play(b, a, GameResult.CHO_WIN); // a 패
        play(b, a, GameResult.HAN_WIN); // a 승
        play(a, b, GameResult.DRAW);
        recorder.startOnline(UUID.randomUUID(), Formation.MA_SANG_MA_SANG, Formation.MA_SANG_MA_SANG, a.getId(),
                b.getId(), 600, 30, 3); // 진행 중은 세지 않는다

        mvc.perform(get("/api/users/" + a.getId() + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(a.getNickname()))
                .andExpect(jsonPath("$.wins").value(2))
                .andExpect(jsonPath("$.losses").value(1))
                .andExpect(jsonPath("$.draws").value(1))
                .andExpect(jsonPath("$.recent.length()").value(5))
                .andExpect(jsonPath("$.recent[1].opponentNickname").value(b.getNickname()));
        mvc.perform(get("/api/users/" + b.getId() + "/profile"))
                .andExpect(jsonPath("$.wins").value(1))
                .andExpect(jsonPath("$.losses").value(2));
    }

    @Test
    void unknownMemberIs404() throws Exception {
        mvc.perform(get("/api/users/999999/profile")).andExpect(status().isNotFound());
    }

    private void play(User cho, User han, GameResult result) {
        UUID id = UUID.randomUUID();
        recorder.startOnline(id, Formation.MA_SANG_MA_SANG, Formation.MA_SANG_MA_SANG, cho.getId(), han.getId(), 600,
                30, 3);
        recorder.finish(id, result, "RESIGN");
    }
}
