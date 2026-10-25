package com.janggi.server.game;

import com.janggi.core.IllegalMoveException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService games;

    public GameController(GameService games) {
        this.games = games;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameDto create(@RequestBody(required = false) NewGameRequest request) {
        NewGameRequest r = request == null ? new NewGameRequest(null, null, null, null) : request;
        return games.create(r.cho(), r.han(), r.aiSide(), r.aiSide() == null ? null : r.level());
    }

    @GetMapping("/{id}")
    public GameDto get(@PathVariable String id) {
        return games.get(id);
    }

    @PostMapping("/{id}/moves")
    public GameDto move(@PathVariable String id, @RequestBody GameDto.MoveDto move) {
        return games.play(id, move.toMove());
    }

    @ExceptionHandler(IllegalMoveException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> illegalMove(IllegalMoveException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(GameNotFoundException e) {
        return Map.of("error", e.getMessage());
    }
}
