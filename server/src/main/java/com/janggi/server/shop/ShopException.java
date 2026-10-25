package com.janggi.server.shop;

/** 살 수 없거나 장착할 수 없을 때. 웹에는 400과 함께 메시지를 보낸다. */
public class ShopException extends RuntimeException {

    public ShopException(String message) {
        super(message);
    }
}
