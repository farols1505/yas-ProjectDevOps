package com.yas.order.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemGetVm;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ViewModelMappingTest {

    @Test
    void errorVm_compactConstructor_fillsEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("404", "T", "D");
        assertThat(vm.fieldErrors()).isEmpty();
    }

    @Test
    void responseStatusVm_holdsValues() {
        ResponeStatusVm vm = new ResponeStatusVm("t", "m", "200");
        assertThat(vm.statusCode()).isEqualTo("200");
    }

    @Test
    void orderVm_fromModel_withNullItems() {
        OrderAddress addr = OrderAddress.builder().id(1L).phone("p").contactName("c").build();
        Order order = Order.builder()
            .id(9L)
            .email("e@e.com")
            .shippingAddressId(addr)
            .billingAddressId(addr)
            .note("n")
            .tax(1f)
            .discount(2f)
            .numberItem(1)
            .totalPrice(BigDecimal.TEN)
            .deliveryFee(BigDecimal.ONE)
            .couponCode("C")
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .checkoutId("chk")
            .build();

        OrderVm vm = OrderVm.fromModel(order, null);

        assertThat(vm.id()).isEqualTo(9L);
        assertThat(vm.orderItemVms()).isNull();
    }

    @Test
    void orderGetVm_fromModel_withEmptyItems() {
        Order order = Order.builder()
            .id(3L)
            .orderStatus(OrderStatus.COMPLETED)
            .totalPrice(BigDecimal.ONE)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .build();
        order.setCreatedOn(ZonedDateTime.now());

        OrderGetVm vm = OrderGetVm.fromModel(order, Collections.emptySet());

        assertThat(vm.orderItems()).isEmpty();
    }

    @Test
    void orderItemGetVm_fromModels_nullCollection() {
        assertThat(OrderItemGetVm.fromModels(null)).isEmpty();
    }

    @Test
    void orderItemVm_fromModel_roundTrip() {
        OrderItem item = OrderItem.builder()
            .id(1L)
            .productId(2L)
            .productName("N")
            .quantity(3)
            .productPrice(BigDecimal.TEN)
            .note("x")
            .discountAmount(BigDecimal.ONE)
            .taxAmount(BigDecimal.ONE)
            .taxPercent(BigDecimal.ZERO)
            .orderId(9L)
            .build();

        OrderItemVm vm = OrderItemVm.fromModel(item);

        assertThat(vm.productId()).isEqualTo(2L);
        assertThat(vm.quantity()).isEqualTo(3);
    }
}
