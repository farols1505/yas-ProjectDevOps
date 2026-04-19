package com.yas.payment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.payment.model.PaymentProvider;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    PaymentProviderMapperImpl.class,
    CreatePaymentProviderMapperImpl.class,
    UpdatePaymentProviderMapperImpl.class
})
class PaymentMapperSpringTest {

    @Autowired
    private PaymentProviderMapper paymentProviderMapper;

    @Autowired
    private CreatePaymentProviderMapper createPaymentProviderMapper;

    @Autowired
    private UpdatePaymentProviderMapper updatePaymentProviderMapper;

    @Test
    void paymentProviderMapper_roundTripEntityAndVm() {
        PaymentProvider entity = PaymentProvider.builder()
            .id("prov-1")
            .enabled(true)
            .name("N")
            .configureUrl("https://c")
            .landingViewComponentName("L")
            .additionalSettings("{}")
            .mediaId(10L)
            .version(2)
            .isNew(false)
            .build();

        PaymentProviderVm vm = paymentProviderMapper.toVm(entity);
        assertThat(vm.getId()).isEqualTo("prov-1");
        assertThat(vm.getName()).isEqualTo("N");
        assertThat(vm.getConfigureUrl()).isEqualTo("https://c");
        assertThat(vm.getVersion()).isEqualTo(2);
        assertThat(vm.getMediaId()).isEqualTo(10L);

        PaymentProvider back = paymentProviderMapper.toModel(vm);
        assertThat(back.getId()).isEqualTo("prov-1");
        assertThat(back.getName()).isEqualTo("N");
        assertThat(back.getConfigureUrl()).isEqualTo("https://c");
        assertThat(back.getMediaId()).isEqualTo(10L);
    }

    @Test
    void createMapper_toModelAndToVmResponse() {
        CreatePaymentVm vm = new CreatePaymentVm();
        vm.setId("new-1");
        vm.setEnabled(true);
        vm.setName("Pay");
        vm.setConfigureUrl("https://x");
        vm.setLandingViewComponentName("comp");
        vm.setAdditionalSettings("{\"a\":1}");
        vm.setMediaId(3L);

        PaymentProvider model = createPaymentProviderMapper.toModel(vm);
        assertThat(model.getId()).isEqualTo("new-1");
        assertThat(model.isNew()).isTrue();

        PaymentProviderVm response = createPaymentProviderMapper.toVmResponse(model);
        assertThat(response.getId()).isEqualTo("new-1");
        assertThat(response.getName()).isEqualTo("Pay");
    }

    @Test
    void updateMapper_partialUpdate_preservesUnspecifiedFields() {
        PaymentProvider entity = PaymentProvider.builder()
            .id("u1")
            .enabled(false)
            .name("Old")
            .configureUrl("https://old")
            .landingViewComponentName("oldComp")
            .additionalSettings("{}")
            .mediaId(1L)
            .version(0)
            .isNew(false)
            .build();

        UpdatePaymentVm patch = new UpdatePaymentVm();
        patch.setId("u1");
        patch.setName("NewName");
        patch.setEnabled(true);
        patch.setConfigureUrl(null);
        patch.setLandingViewComponentName(null);

        updatePaymentProviderMapper.partialUpdate(entity, patch);

        assertThat(entity.getName()).isEqualTo("NewName");
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.getConfigureUrl()).isEqualTo("https://old");

        PaymentProviderVm out = updatePaymentProviderMapper.toVmResponse(entity);
        assertThat(out.getName()).isEqualTo("NewName");
    }
}
