package com.yas.payment.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void compactConstructor_initializesEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("400", "Bad", "detail");
        assertThat(vm.fieldErrors()).isEmpty();
    }
}
