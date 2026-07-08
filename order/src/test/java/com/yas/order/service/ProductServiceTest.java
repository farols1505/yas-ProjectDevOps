package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class ProductServiceTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        setUpSecurityContext("u1");
        when(serviceUrlConfig.product()).thenReturn("http://api.yas.local/product");
    }

    @Test
    void getProductVariations_returnsBody() {
        List<ProductVariationVm> body = List.of(new ProductVariationVm(1L, "n", "s"));
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        ResponseEntity<List<ProductVariationVm>> entity = ResponseEntity.ok(body);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(URI.class))).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(entity);

        assertThat(productService.getProductVariations(5L)).isEqualTo(body);
    }

    @Test
    void subtractProductStockQuantity_invokesPut() {
        RestClient.RequestBodyUriSpec putSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        OrderVm vm = sampleOrderVm();

        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.uri(any(URI.class))).thenReturn(putSpec);
        when(putSpec.headers(any())).thenReturn(putSpec);
        when(putSpec.body(ArgumentMatchers.<List<?>>anyList())).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);

        productService.subtractProductStockQuantity(vm);

        Mockito.verify(putSpec).body(ArgumentMatchers.anyList());
    }

    @Test
    void getProductInfomation_mapsById() {
        ProductCheckoutListVm p1 = ProductCheckoutListVm.builder().id(10L).name("A").price(1.0).taxClassId(1L).build();
        ProductGetCheckoutListVm body = new ProductGetCheckoutListVm(List.of(p1), 0, 1, 1, 1, true);

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        ResponseEntity<ProductGetCheckoutListVm> entity = ResponseEntity.ok(body);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(URI.class))).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(entity);

        Map<Long, ProductCheckoutListVm> map = productService.getProductInfomation(Set.of(10L), 0, 10);

        assertThat(map).containsKey(10L);
        assertThat(map.get(10L).getName()).isEqualTo("A");
    }

    @Test
    void getProductInfomation_whenBodyNull_throws() {
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        ResponseEntity<ProductGetCheckoutListVm> entity = ResponseEntity.ok(null);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(URI.class))).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(entity);

        assertThrows(NotFoundException.class, () -> productService.getProductInfomation(Set.of(1L), 0, 5));
    }

    @Test
    void getProductInfomation_whenListNull_throws() {
        ProductGetCheckoutListVm body = new ProductGetCheckoutListVm(null, 0, 0, 0, 0, true);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        ResponseEntity<ProductGetCheckoutListVm> entity = ResponseEntity.ok(body);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(URI.class))).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(entity);

        assertThrows(NotFoundException.class, () -> productService.getProductInfomation(Set.of(1L), 0, 5));
    }

    @Test
    void handleProductVariationListFallback_rethrows() {
        ProductServiceExposed exposed = new ProductServiceExposed(restClient, serviceUrlConfig);
        RuntimeException ex = new RuntimeException("cb");
        assertThrows(RuntimeException.class, () -> exposed.productVariationFallback(ex));
    }

    @Test
    void handleProductInformationFallback_rethrows() {
        ProductServiceExposed exposed = new ProductServiceExposed(restClient, serviceUrlConfig);
        assertThrows(RuntimeException.class, () -> exposed.productInfoFallback(new RuntimeException("cb")));
    }

    @Test
    void handleBodilessFallback_rethrows() {
        ProductServiceExposed exposed = new ProductServiceExposed(restClient, serviceUrlConfig);
        assertThrows(RuntimeException.class, () -> exposed.bodilessFallback(new RuntimeException("cb")));
    }

    private static OrderVm sampleOrderVm() {
        OrderItemVm item = new OrderItemVm(
            1L, 10L, "P", 2, BigDecimal.ONE, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1L);
        return new OrderVm(
            1L, "e@e.com", null, null, "", 0f, 0f, 1, BigDecimal.TEN, BigDecimal.ZERO, "",
            OrderStatus.PENDING, DeliveryMethod.YAS_EXPRESS, DeliveryStatus.PREPARING, PaymentStatus.PENDING,
            new HashSet<>(Set.of(item)), "chk");
    }

    private static final class ProductServiceExposed extends ProductService {
        ProductServiceExposed(RestClient r, ServiceUrlConfig c) {
            super(r, c);
        }

        List<ProductVariationVm> productVariationFallback(Throwable t) throws Throwable {
            return handleProductVariationListFallback(t);
        }

        Map<Long, ProductCheckoutListVm> productInfoFallback(Throwable t) throws Throwable {
            return handleProductInfomationFallback(t);
        }

        void bodilessFallback(Throwable t) throws Throwable {
            handleBodilessFallback(t);
        }
    }
}
