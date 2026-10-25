package com.janggi.server.shop;

/**
 * 돈이 오가지 않는 테스트 결제. 실제 결제사를 붙이기 전까지 쓴다.
 * paymentKey가 "fail"로 시작하면 거절해서, 실패 화면도 확인할 수 있다.
 */
public class TestPaymentGateway implements PaymentGateway {

    @Override
    public String name() {
        return "test";
    }

    @Override
    public void confirm(String orderId, String paymentKey, int amount) {
        if (paymentKey == null || paymentKey.isBlank() || paymentKey.startsWith("fail")) {
            throw new ShopException("결제가 승인되지 않았습니다");
        }
    }
}
