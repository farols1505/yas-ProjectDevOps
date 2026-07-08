package com.yas.payment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void errorCodes_defined() {
        assertThat(Constants.ErrorCode.PAYMENT_PROVIDER_NOT_FOUND).isEqualTo("PAYMENT_PROVIDER_NOT_FOUND");
    }

    @Test
    void messageConstants_defined() {
        assertThat(Constants.Message.SUCCESS_MESSAGE).isEqualTo("SUCCESS");
    }
}
