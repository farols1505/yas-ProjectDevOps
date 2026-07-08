package com.yas.inventory.viewmodel.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructorWithThreeArgs_initializesEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("400", "Bad", "detail");

        assertThat(vm.statusCode()).isEqualTo("400");
        assertThat(vm.title()).isEqualTo("Bad");
        assertThat(vm.detail()).isEqualTo("detail");
        assertThat(vm.fieldErrors()).isEmpty();
    }

    @Test
    void fullConstructor_preservesFieldErrors() {
        ErrorVm vm = new ErrorVm("422", "Unprocessable", "d", java.util.List.of("f1"));

        assertThat(vm.fieldErrors()).containsExactly("f1");
    }
}
