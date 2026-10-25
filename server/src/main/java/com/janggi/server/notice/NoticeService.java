package com.janggi.server.notice;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    public static final int MAX_TITLE = 60;
    public static final int MAX_BODY = 2000;

    /** 사용자에게 보이는 공지. updatedAt이 바뀌면 웹은 '오늘 그만 보기'를 한 공지도 다시 띄운다. */
    public record NoticeDto(long id, String title, String body, OffsetDateTime startsAt, OffsetDateTime endsAt,
            boolean active, OffsetDateTime updatedAt) {

        static NoticeDto of(Notice n) {
            return new NoticeDto(n.getId(), n.getTitle(), n.getBody(), n.getStartsAt(), n.getEndsAt(), n.isActive(),
                    n.getUpdatedAt());
        }
    }

    /** 관리자가 보내는 공지 내용. startsAt이 비면 지금부터, endsAt이 비면 끄기 전까지 보인다. */
    public record NoticeRequest(String title, String body, OffsetDateTime startsAt, OffsetDateTime endsAt,
            Boolean active) {
    }

    public static class InvalidNotice extends RuntimeException {
        InvalidNotice(String message) {
            super(message);
        }
    }

    private final NoticeRepository notices;

    public NoticeService(NoticeRepository notices) {
        this.notices = notices;
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> showing() {
        return notices.showing(OffsetDateTime.now()).stream().map(NoticeDto::of).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> all() {
        return notices.findTop100ByOrderByStartsAtDescIdDesc().stream().map(NoticeDto::of).toList();
    }

    @Transactional
    public NoticeDto create(long adminId, NoticeRequest req) {
        OffsetDateTime now = OffsetDateTime.now();
        Notice n = new Notice(adminId, now);
        apply(n, req, now);
        return NoticeDto.of(notices.save(n));
    }

    @Transactional
    public NoticeDto update(long id, NoticeRequest req) {
        Notice n = find(id);
        apply(n, req, OffsetDateTime.now());
        return NoticeDto.of(n);
    }

    /** 지운 공지의 제목을 돌려준다(관리자 기록용). */
    @Transactional
    public String delete(long id) {
        Notice n = find(id);
        notices.delete(n);
        return n.getTitle();
    }

    private Notice find(long id) {
        return notices.findById(id).orElseThrow(() -> new InvalidNotice("공지를 찾을 수 없습니다"));
    }

    private static void apply(Notice n, NoticeRequest req, OffsetDateTime now) {
        String title = req.title() == null ? "" : req.title().strip();
        String body = req.body() == null ? "" : req.body().strip();
        if (title.isEmpty() || title.length() > MAX_TITLE) {
            throw new InvalidNotice("제목은 1~" + MAX_TITLE + "자로 적어 주세요");
        }
        if (body.isEmpty() || body.length() > MAX_BODY) {
            throw new InvalidNotice("내용은 1~" + MAX_BODY + "자로 적어 주세요");
        }
        OffsetDateTime starts = req.startsAt() == null ? now : req.startsAt();
        if (req.endsAt() != null && !req.endsAt().isAfter(starts)) {
            throw new InvalidNotice("끝나는 때는 시작보다 뒤여야 해요");
        }
        n.edit(title, body, starts, req.endsAt(), req.active() == null || req.active(), now);
    }
}
