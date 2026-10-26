package com.janggi.server.user;

/** 캐릭터 아바타 차림. 모두 상점 상품 id이고, style은 화풍, background는 배경이다. */
public record Look(String style, String background, String body, String hair, String beard, String outfit, String hat) {

    public static Look of(User u) {
        return new Look(u.getStyle(), u.getAvatar(), u.getBody(), u.getHair(), u.getBeard(), u.getOutfit(), u.getHat());
    }
}
