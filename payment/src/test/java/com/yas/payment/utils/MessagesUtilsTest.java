package com.yas.payment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_resolvesKnownKey() {
        String msg = MessagesUtils.getMessage("PAYMENT_PROVIDER_NOT_FOUND", "abc");
        assertThat(msg).contains("abc");
    }

    @Test
    void getMessage_unknownKey_returnsCode() {
        String msg = MessagesUtils.getMessage("UNKNOWN_CODE_XYZ");
        assertThat(msg).isEqualTo("UNKNOWN_CODE_XYZ");
    }
}
