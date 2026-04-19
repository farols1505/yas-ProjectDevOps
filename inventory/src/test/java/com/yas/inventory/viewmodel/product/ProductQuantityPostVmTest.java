package com.yas.inventory.viewmodel.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import org.junit.jupiter.api.Test;

class ProductQuantityPostVmTest {

    @Test
    void fromModel_mapsProductIdAndQuantity() {
        Warehouse wh = Warehouse.builder().id(1L).name("W").addressId(1L).build();
        Stock stock = Stock.builder()
            .id(9L)
            .productId(100L)
            .quantity(55L)
            .reservedQuantity(0L)
            .warehouse(wh)
            .build();

        ProductQuantityPostVm vm = ProductQuantityPostVm.fromModel(stock);

        assertThat(vm.productId()).isEqualTo(100L);
        assertThat(vm.stockQuantity()).isEqualTo(55L);
    }
}
