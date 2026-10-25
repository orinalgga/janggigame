package com.janggi.server.shop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopConfig {

    /** 지금은 테스트 결제뿐이다. 토스페이먼츠 같은 실제 결제사는 provider 값으로 골라 붙인다. */
    @Bean
    PaymentGateway paymentGateway(@Value("${janggi.payments.provider:test}") String provider) {
        if (!provider.equals("test")) {
            throw new IllegalStateException("지원하지 않는 결제사: " + provider);
        }
        return new TestPaymentGateway();
    }
}
