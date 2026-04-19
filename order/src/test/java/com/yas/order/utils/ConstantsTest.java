package com.yas.order.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void errorCodes_areStable() {
        assertThat(Constants.ErrorCode.ORDER_NOT_FOUND).isEqualTo("ORDER_NOT_FOUND");
        assertThat(Constants.ErrorCode.CHECKOUT_NOT_FOUND).isEqualTo("CHECKOUT_NOT_FOUND");
    }

    @Test
    void columnNames_matchEntityFields() {
        assertThat(Constants.Column.ORDER_EMAIL_COLUMN).isEqualTo("email");
        assertThat(Constants.Column.CREATE_ON_COLUMN).isEqualTo("createdOn");
    }
}
