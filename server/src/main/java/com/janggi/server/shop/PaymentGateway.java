package com.janggi.server.shop;

/**
 * 결제사. 웹(또는 앱)이 결제창에서 받은 paymentKey를 서버가 결제사에 보내 승인받는다.
 * 금액은 웹이 보낸 값이 아니라 서버의 주문 금액으로 승인한다.
 */
public interface PaymentGateway {

    /** 주문에 남길 결제사 이름. */
    String name();

    /** 승인되면 그냥 돌아오고, 거절되면 ShopException을 던진다. */
    void confirm(String orderId, String paymentKey, int amount);
}
