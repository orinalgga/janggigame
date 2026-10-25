package com.janggi.server.notice;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 지금 보이는 공지. 로그인하지 않아도 본다. */
@RestController
public class NoticeController {

    private final NoticeService notices;

    public NoticeController(NoticeService notices) {
        this.notices = notices;
    }

    @GetMapping("/api/notices")
    public List<NoticeService.NoticeDto> showing() {
        return notices.showing();
    }
}
