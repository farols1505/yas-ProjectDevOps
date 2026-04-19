package com.yas.payment.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentEntityTest {

    @Test
    void builder_populatesFields() {
        Payment p = Payment.builder()
            .id(1L)
            .orderId(2L)
            .checkoutId("chk")
            .amount(BigDecimal.TEN)
            .paymentFee(BigDecimal.ONE)
            .paymentMethod(PaymentMethod.PAYPAL)
            .paymentStatus(PaymentStatus.COMPLETED)
            .gatewayTransactionId("gw")
            .failureMessage(null)
            .build();

        assertThat(p.getCheckoutId()).isEqualTo("chk");
        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
