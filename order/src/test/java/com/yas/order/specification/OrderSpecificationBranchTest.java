package com.yas.order.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.order.model.Order;
import com.yas.order.model.enumeration.OrderStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class OrderSpecificationBranchTest {

    private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    private final Root<Order> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    @Test
    void hasOrderStatus_null_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.hasOrderStatus(null).toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withEmail_empty_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withEmail("").toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withOrderStatus_empty_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withOrderStatus(List.of()).toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withBillingPhoneNumber_blank_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withBillingPhoneNumber("").toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withCountryName_null_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withCountryName(null).toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withDateRange_partial_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withDateRange(null, null).toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void hasProductInOrderItems_queryNull_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.hasProductInOrderItems(List.of(1L)).toPredicate(root, null, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withProductName_queryNull_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withProductName("x").toPredicate(root, null, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void withProductName_emptyName_returnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        Predicate p = OrderSpecification.withProductName("").toPredicate(root, query, criteriaBuilder);
        assertNotNull(p);
    }

    @Test
    void findOrderByWithMulCriteria_countQuery_skipsFetch() {
        when(query.getResultType()).thenReturn((Class) Long.class);
        when(root.get(any(String.class))).thenReturn(mock(Path.class));
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(), any(), any(), any(), any(), any())).thenReturn(mock(Predicate.class));

        Specification<Order> spec = OrderSpecification.findOrderByWithMulCriteria(
            List.of(OrderStatus.PENDING),
            "",
            "",
            "",
            "",
            null,
            null
        );
        assertNotNull(spec.toPredicate(root, query, criteriaBuilder));
    }

    @Test
    void findOrderByWithMulCriteria_detailQuery_fetchesAddresses() {
        when(query.getResultType()).thenReturn((Class) Order.class);
        @SuppressWarnings("unchecked")
        Fetch<Object, Object> shipFetch = mock(Fetch.class);
        @SuppressWarnings("unchecked")
        Fetch<Object, Object> billFetch = mock(Fetch.class);
        when(root.fetch(eq("shippingAddressId"), any())).thenReturn(shipFetch);
        when(root.fetch(eq("billingAddressId"), any())).thenReturn(billFetch);
        when(root.get(any(String.class))).thenReturn(mock(Path.class));
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(), any(), any(), any(), any(), any())).thenReturn(mock(Predicate.class));

        Specification<Order> spec = OrderSpecification.findOrderByWithMulCriteria(
            List.of(OrderStatus.ACCEPTED),
            "09",
            "VN",
            "a@b.com",
            "",
            null,
            null
        );
        assertNotNull(spec.toPredicate(root, query, criteriaBuilder));
    }
}
