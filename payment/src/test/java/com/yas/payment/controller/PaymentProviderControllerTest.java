package com.yas.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.util.List;
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

@WebMvcTest(controllers = PaymentProviderController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentProviderControllerTest {

    @MockitoBean
    private PaymentProviderService paymentProviderService;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpMapper() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void create_returns201() throws Exception {
        CreatePaymentVm body = new CreatePaymentVm();
        body.setId("p1");
        body.setName("PayPal");
        body.setEnabled(true);
        body.setConfigureUrl("https://cfg");
        body.setLandingViewComponentName("Landing");
        PaymentProviderVm vm = new PaymentProviderVm("p1", "PayPal", "https://cfg", 0, null, null);
        when(paymentProviderService.create(any(CreatePaymentVm.class))).thenReturn(vm);

        mockMvc.perform(post("/backoffice/payment-providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("p1"))
            .andExpect(jsonPath("$.name").value("PayPal"));
    }

    @Test
    void update_returns200() throws Exception {
        UpdatePaymentVm body = new UpdatePaymentVm();
        body.setId("p1");
        body.setName("PayPal");
        body.setEnabled(true);
        body.setConfigureUrl("https://cfg");
        body.setLandingViewComponentName("Landing");
        PaymentProviderVm vm = new PaymentProviderVm("p1", "PayPal", "https://cfg", 1, 5L, "https://icon");
        when(paymentProviderService.update(any(UpdatePaymentVm.class))).thenReturn(vm);

        mockMvc.perform(put("/backoffice/payment-providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("p1"));
    }

    @Test
    void getAll_returnsList() throws Exception {
        PaymentProviderVm vm = new PaymentProviderVm("p1", "PayPal", "https://cfg", 0, null, null);
        when(paymentProviderService.getEnabledPaymentProviders(any())).thenReturn(List.of(vm));

        mockMvc.perform(get("/storefront/payment-providers").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("p1"));
    }
}
