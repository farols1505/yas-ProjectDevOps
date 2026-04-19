package com.yas.payment.service.provider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaypalHandlerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaypalService paypalService;

    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
        lenient().when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("{\"sandbox\":true}");
    }

    @Test
    void initPayment_delegatesToPaypalService() {
        InitPaymentRequestVm req = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(new BigDecimal("50.00"))
            .checkoutId("chk-1")
            .build();
        PaypalCreatePaymentResponse paypalResp = PaypalCreatePaymentResponse.builder()
            .status("CREATED")
            .paymentId("PAY-1")
            .redirectUrl("https://paypal.example/approve")
            .build();
        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class))).thenReturn(paypalResp);

        InitiatedPayment result = paypalHandler.initPayment(req);

        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getPaymentId()).isEqualTo("PAY-1");
        assertThat(result.getRedirectUrl()).isEqualTo("https://paypal.example/approve");
        verify(paypalService).createPayment(any(PaypalCreatePaymentRequest.class));
    }

    @Test
    void capturePayment_mapsPaypalResponseToCapturedPayment() {
        CapturePaymentRequestVm req = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("EC-TOKEN")
            .build();
        PaypalCapturePaymentResponse paypalResp = PaypalCapturePaymentResponse.builder()
            .checkoutId("chk-2")
            .amount(new BigDecimal("10.00"))
            .paymentFee(BigDecimal.ONE)
            .gatewayTransactionId("gw-99")
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .failureMessage(null)
            .build();
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class))).thenReturn(paypalResp);

        CapturedPayment result = paypalHandler.capturePayment(req);

        assertThat(result.getCheckoutId()).isEqualTo("chk-2");
        assertThat(result.getAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getGatewayTransactionId()).isEqualTo("gw-99");
        verify(paypalService).capturePayment(any(PaypalCapturePaymentRequest.class));
    }

    @Test
    void getProviderId_returnsPaypal() {
        assertThat(paypalHandler.getProviderId()).isEqualTo(PaymentMethod.PAYPAL.name());
    }
}
