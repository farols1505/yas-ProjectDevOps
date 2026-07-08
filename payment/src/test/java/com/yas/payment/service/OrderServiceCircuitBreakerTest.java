package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.payment.config.ServiceUrlConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

class OrderServiceCircuitBreakerTest {

    @Test
    void handleLongFallback_rethrows() {
        OrderServiceExposed svc = new OrderServiceExposed(
            Mockito.mock(RestClient.class),
            Mockito.mock(ServiceUrlConfig.class)
        );
        assertThrows(RuntimeException.class, () -> svc.invokeLongFallback(new RuntimeException("cb")));
    }

    @Test
    void handlePaymentOrderStatusFallback_rethrows() {
        OrderServiceExposed svc = new OrderServiceExposed(
            Mockito.mock(RestClient.class),
            Mockito.mock(ServiceUrlConfig.class)
        );
        assertThrows(RuntimeException.class, () -> svc.invokePaymentStatusFallback(new RuntimeException("cb")));
    }

    private static final class OrderServiceExposed extends OrderService {
        OrderServiceExposed(RestClient restClient, ServiceUrlConfig serviceUrlConfig) {
            super(restClient, serviceUrlConfig);
        }

        Long invokeLongFallback(Throwable t) throws Throwable {
            return handleLongFallback(t);
        }

        com.yas.payment.viewmodel.PaymentOrderStatusVm invokePaymentStatusFallback(Throwable t) throws Throwable {
            return handlePaymentOrderStatusFallback(t);
        }
    }
}
