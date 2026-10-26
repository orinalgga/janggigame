package com.janggi.server.user;

/** 캐릭터 아바타 차림. 모두 상점 상품 id이고, background는 배경 패(예전 아바타)다. */
public record Look(String background, String body, String hair, String beard, String outfit, String hat) {

    public static Look of(User u) {
        return new Look(u.getAvatar(), u.getBody(), u.getHair(), u.getBeard(), u.getOutfit(), u.getHat());
    }
}
