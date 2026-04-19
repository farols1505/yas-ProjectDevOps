package com.yas.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.PaymentService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

@WebMvcTest(controllers = PaymentController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    private ObjectWriter objectWriter;

    @BeforeEach
    void setUp() {
        objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    @Test
    void initPayment_returnsBody() throws Exception {
        InitPaymentRequestVm req = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(BigDecimal.TEN)
            .checkoutId("c1")
            .build();
        InitPaymentResponseVm res = InitPaymentResponseVm.builder()
            .status("OK")
            .paymentId("p1")
            .redirectUrl("https://r")
            .build();
        when(paymentService.initPayment(any(InitPaymentRequestVm.class))).thenReturn(res);

        mockMvc.perform(post("/init")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(content().json(objectWriter.writeValueAsString(res)));
    }

    @Test
    void capturePayment_returnsBody() throws Exception {
        CapturePaymentRequestVm req = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("t1")
            .build();
        CapturePaymentResponseVm res = CapturePaymentResponseVm.builder()
            .orderId(1L)
            .checkoutId("c1")
            .amount(BigDecimal.ONE)
            .paymentFee(BigDecimal.ZERO)
            .gatewayTransactionId("g1")
            .paymentMethod(PaymentMethod.PAYPAL)
            .paymentStatus(PaymentStatus.COMPLETED)
            .failureMessage(null)
            .build();
        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(res);

        mockMvc.perform(post("/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(content().json(objectWriter.writeValueAsString(res)));
    }

    @Test
    void cancelPayment_returnsOk() throws Exception {
        mockMvc.perform(get("/cancel"))
            .andExpect(status().isOk())
            .andExpect(content().string("Payment cancelled"));
    }
}
