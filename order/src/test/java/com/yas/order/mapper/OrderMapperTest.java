package com.yas.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = OrderMapperImpl.class)
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void toCsv_mapsBillingPhoneAndCoreFields() {
        OrderAddressVm billing = OrderAddressVm.builder()
            .id(2L)
            .contactName("Jane")
            .phone("+84901234567")
            .addressLine1("L1")
            .addressLine2("L2")
            .city("HCMC")
            .zipCode("70000")
            .districtId(1L)
            .districtName("D1")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("S1")
            .countryId(3L)
            .countryName("VN")
            .build();

        OrderBriefVm brief = OrderBriefVm.builder()
            .id(42L)
            .email("buyer@example.com")
            .billingAddressVm(billing)
            .totalPrice(new BigDecimal("120.50"))
            .orderStatus(OrderStatus.PAID)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.COMPLETED)
            .createdOn(ZonedDateTime.parse("2024-01-15T10:15:30+07:00"))
            .build();

        OrderItemCsv csv = orderMapper.toCsv(brief);

        assertThat(csv.getId()).isEqualTo(42L);
        assertThat(csv.getEmail()).isEqualTo("buyer@example.com");
        assertThat(csv.getPhone()).isEqualTo("+84901234567");
        assertThat(csv.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(csv.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(csv.getTotalPrice()).isEqualByComparingTo("120.50");
    }
}
