package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import com.yas.order.viewmodel.promotion.PromotionUsageVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_persistsItems_updatesStockCartPromotionAndAccepts() {
        OrderAddressPostVm addr = sampleAddressPostVm();
        OrderPostVm postVm = OrderPostVm.builder()
            .checkoutId("chk-1")
            .email("a@b.com")
            .shippingAddressPostVm(addr)
            .billingAddressPostVm(addr)
            .note("n")
            .tax(1f)
            .discount(2f)
            .numberItem(1)
            .totalPrice(BigDecimal.TEN)
            .deliveryFee(BigDecimal.ONE)
            .couponCode("PROMO")
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .paymentMethod(PaymentMethod.BANKING)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(
                OrderItemPostVm.builder()
                    .productId(10L)
                    .productName("P")
                    .quantity(1)
                    .productPrice(BigDecimal.ONE)
                    .note("")
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .taxPercent(BigDecimal.ZERO)
                    .build()
            ))
            .build();

        AtomicReference<Order> persisted = new AtomicReference<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            persisted.set(o);
            return o;
        });
        when(orderRepository.findById(eq(500L))).thenAnswer(inv -> Optional.ofNullable(persisted.get()));

        OrderVm result = orderService.createOrder(postVm);

        assertNotNull(result);
        assertEquals(500L, result.id());
        verify(orderItemRepository).saveAll(any(Collection.class));
        verify(productService).subtractProductStockQuantity(any(OrderVm.class));
        verify(cartService).deleteCartItems(any(OrderVm.class));

        ArgumentCaptor<List<PromotionUsageVm>> promoCap = ArgumentCaptor.forClass(List.class);
        verify(promotionService).updateUsagePromotion(promoCap.capture());
        assertThat(promoCap.getValue()).hasSize(1);
        assertEquals(10L, promoCap.getValue().getFirst().productId());
        assertEquals(500L, promoCap.getValue().getFirst().orderId());
        assertEquals("PROMO", promoCap.getValue().getFirst().promotionCode());
    }

    @Test
    void getOrderWithItemsById_whenFound_returnsVm() {
        OrderAddress bill = OrderAddress.builder().id(1L).phone("1").countryName("VN").build();
        Order order = Order.builder()
            .id(7L)
            .email("e@e.com")
            .billingAddressId(bill)
            .shippingAddressId(bill)
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .totalPrice(BigDecimal.ONE)
            .build();
        order.setCreatedOn(ZonedDateTime.now());

        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(7L)).thenReturn(List.of(
            OrderItem.builder()
                .id(1L)
                .orderId(7L)
                .productId(2L)
                .productName("X")
                .quantity(1)
                .productPrice(BigDecimal.TEN)
                .note("")
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .taxPercent(BigDecimal.ZERO)
                .build()
        ));

        OrderVm vm = orderService.getOrderWithItemsById(7L);

        assertEquals(7L, vm.id());
        assertThat(vm.orderItemVms()).hasSize(1);
    }

    @Test
    void getOrderWithItemsById_whenMissing_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(1L));
    }

    @Test
    void getAllOrder_whenEmpty_returnsEmptyListVm() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(Page.empty());

        OrderListVm list = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "",
            List.of(),
            Pair.of("", ""),
            "",
            Pair.of(0, 20)
        );

        assertThat(list.orderList()).isNull();
        assertEquals(0, list.totalElements());
    }

    @Test
    void getAllOrder_whenHasContent_returnsBriefVms() {
        OrderAddress bill = OrderAddress.builder().id(1L).phone("p").countryName("VN").build();
        Order order = Order.builder()
            .id(9L)
            .email("x@y.com")
            .billingAddressId(bill)
            .shippingAddressId(bill)
            .orderStatus(OrderStatus.COMPLETED)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .totalPrice(new BigDecimal("99.00"))
            .build();
        order.setCreatedOn(ZonedDateTime.now());

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(order)));

        OrderListVm list = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "prod",
            List.of(OrderStatus.COMPLETED),
            Pair.of("VN", "p"),
            "x@y.com",
            Pair.of(0, 10)
        );

        assertThat(list.orderList()).hasSize(1);
        assertEquals(9L, list.orderList().getFirst().id());
        assertEquals(1, list.totalPages());
    }

    @Test
    void getLatestOrders_nonPositive_returnsEmpty() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();
        assertThat(orderService.getLatestOrders(-3)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryEmpty_returnsEmpty() {
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of());
        assertThat(orderService.getLatestOrders(5)).isEmpty();
    }

    @Test
    void getLatestOrders_mapsOrders() {
        Order o = Order.builder()
            .id(3L)
            .email("m@n.com")
            .billingAddressId(OrderAddress.builder().id(1L).build())
            .orderStatus(OrderStatus.PAID)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.COMPLETED)
            .totalPrice(BigDecimal.ONE)
            .build();
        o.setCreatedOn(ZonedDateTime.now());
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of(o));

        List<OrderBriefVm> brief = orderService.getLatestOrders(3);

        assertThat(brief).hasSize(1);
        assertEquals(3L, brief.getFirst().id());
    }

    @Test
    void isOrderCompleted_whenNoVariations_usesProductId() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(100L)).thenReturn(List.of());
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.of(Order.builder().build()));

        OrderExistsByProductAndUserGetVm vm = orderService.isOrderCompletedWithUserIdAndProductId(100L);

        assertTrue(vm.isPresent());
    }

    @Test
    void isOrderCompleted_whenVariations_usesVariationIds() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(100L)).thenReturn(List.of(
            new ProductVariationVm(201L, "a", "s"),
            new ProductVariationVm(202L, "b", "t")
        ));
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        OrderExistsByProductAndUserGetVm vm = orderService.isOrderCompletedWithUserIdAndProductId(100L);

        assertFalse(vm.isPresent());
    }

    @Test
    void getMyOrders_returnsMappedList() {
        setSubjectUpSecurityContext("user-1");
        Order order = Order.builder()
            .id(11L)
            .orderStatus(OrderStatus.COMPLETED)
            .totalPrice(BigDecimal.TEN)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .build();
        order.setCreatedOn(ZonedDateTime.now());
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
            .thenReturn(List.of(order));

        List<OrderGetVm> vms = orderService.getMyOrders("phone", OrderStatus.COMPLETED);

        assertThat(vms).hasSize(1);
        assertEquals(11L, vms.getFirst().id());
    }

    @Test
    void findOrderVmByCheckoutId_delegatesToRepositories() {
        Order order = Order.builder().id(22L).checkoutId("c1").build();
        when(orderRepository.findByCheckoutId("c1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(22L)).thenReturn(Collections.emptyList());

        OrderGetVm vm = orderService.findOrderVmByCheckoutId("c1");

        assertEquals(22L, vm.id());
    }

    @Test
    void findOrderByCheckoutId_whenMissing_throws() {
        when(orderRepository.findByCheckoutId("x")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("x"));
    }

    @Test
    void updateOrderPaymentStatus_completed_setsPaid() {
        Order order = Order.builder()
            .id(1L)
            .orderStatus(OrderStatus.PENDING)
            .paymentStatus(PaymentStatus.PENDING)
            .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm req = PaymentOrderStatusVm.builder()
            .orderId(1L)
            .orderStatus("ignored")
            .paymentId(99L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build();

        PaymentOrderStatusVm res = orderService.updateOrderPaymentStatus(req);

        assertEquals(OrderStatus.PAID.getName(), res.orderStatus());
        assertEquals(PaymentStatus.COMPLETED.name(), res.paymentStatus());
    }

    @Test
    void updateOrderPaymentStatus_whenOrderMissing_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentOrderStatusVm req = PaymentOrderStatusVm.builder()
            .orderId(1L)
            .paymentStatus(PaymentStatus.PENDING.name())
            .paymentId(1L)
            .build();
        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(req));
    }

    @Test
    void rejectOrder_updatesStatus() {
        Order order = Order.builder().id(3L).orderStatus(OrderStatus.PENDING).build();
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(3L, "bad");

        assertEquals(OrderStatus.REJECT, order.getOrderStatus());
        assertEquals("bad", order.getRejectReason());
        verify(orderRepository).save(order);
    }

    @Test
    void acceptOrder_updatesStatus() {
        Order order = Order.builder().id(4L).orderStatus(OrderStatus.PENDING).build();
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(4L);

        assertEquals(OrderStatus.ACCEPTED, order.getOrderStatus());
    }

    @Test
    void exportCsv_whenNoOrders_returnsEmptyCsvBytes() throws IOException {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(Page.empty());

        byte[] bytes = orderService.exportCsv(OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .productName("")
            .orderStatus(List.of())
            .billingPhoneNumber("")
            .billingCountry("")
            .email("")
            .pageNo(0)
            .pageSize(10)
            .build());

        assertNotNull(bytes);
        assertThat(bytes.length).isPositive();
    }

    @Test
    void exportCsv_whenOrders_present_usesMapper() throws IOException {
        OrderAddress bill = OrderAddress.builder().id(1L).phone("p").countryName("VN").build();
        Order order = Order.builder()
            .id(9L)
            .email("csv@test.com")
            .billingAddressId(bill)
            .shippingAddressId(bill)
            .orderStatus(OrderStatus.COMPLETED)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .totalPrice(BigDecimal.TEN)
            .build();
        order.setCreatedOn(ZonedDateTime.now());

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(order)));

        OrderItemCsv csvRow = OrderItemCsv.builder()
            .id(9L)
            .orderStatus(OrderStatus.COMPLETED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .email("csv@test.com")
            .phone("p")
            .totalPrice(BigDecimal.TEN)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .createdOn(order.getCreatedOn())
            .build();
        when(orderMapper.toCsv(any(OrderBriefVm.class))).thenReturn(csvRow);

        byte[] bytes = orderService.exportCsv(OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .productName("x")
            .orderStatus(List.of(OrderStatus.COMPLETED))
            .billingPhoneNumber("p")
            .billingCountry("VN")
            .email("csv")
            .pageNo(0)
            .pageSize(5)
            .build());

        assertNotNull(bytes);
        verify(orderMapper).toCsv(any(OrderBriefVm.class));
    }

    private static OrderAddressPostVm sampleAddressPostVm() {
        return OrderAddressPostVm.builder()
            .contactName("c")
            .phone("1")
            .addressLine1("a1")
            .addressLine2("a2")
            .city("ct")
            .zipCode("z")
            .districtId(1L)
            .districtName("d")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("s")
            .countryId(3L)
            .countryName("VN")
            .build();
    }
}
