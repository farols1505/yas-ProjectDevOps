package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    static final class TestHandler extends AbstractCircuitBreakFallbackHandler {
        void invokeBodiless(Throwable t) throws Throwable {
            handleBodilessFallback(t);
        }

        <T> T invokeTyped(Throwable t) throws Throwable {
            return handleTypedFallback(t);
        }
    }

    @Test
    void handleBodilessFallback_rethrowsOriginal() {
        TestHandler handler = new TestHandler();
        IllegalStateException cause = new IllegalStateException("cb");

        assertThrows(IllegalStateException.class, () -> handler.invokeBodiless(cause));
    }

    @Test
    void handleTypedFallback_rethrowsOriginal() {
        TestHandler handler = new TestHandler();
        IllegalStateException cause = new IllegalStateException("cb");

        assertThrows(IllegalStateException.class, () -> handler.invokeTyped(cause));
    }
}
