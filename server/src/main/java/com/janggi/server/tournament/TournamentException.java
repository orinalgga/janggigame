package com.janggi.server.tournament;

/** 대회에 참가하거나 대회를 만들 수 없을 때. 웹에는 400과 함께 메시지를 보낸다. */
public class TournamentException extends RuntimeException {

    public TournamentException(String message) {
        super(message);
    }
}
