package com.yas.inventory.viewmodel.stock;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import org.junit.jupiter.api.Test;

class StockVmTest {

    @Test
    void fromModel_mapsAllFields() {
        Warehouse wh = Warehouse.builder().id(7L).name("W").addressId(1L).build();
        Stock stock = Stock.builder()
            .id(11L)
            .productId(22L)
            .quantity(3L)
            .reservedQuantity(2L)
            .warehouse(wh)
            .build();
        ProductInfoVm product = new ProductInfoVm(22L, "Name", "SKU-1", true);

        StockVm vm = StockVm.fromModel(stock, product);

        assertThat(vm.id()).isEqualTo(11L);
        assertThat(vm.productId()).isEqualTo(22L);
        assertThat(vm.productName()).isEqualTo("Name");
        assertThat(vm.productSku()).isEqualTo("SKU-1");
        assertThat(vm.quantity()).isEqualTo(3L);
        assertThat(vm.reservedQuantity()).isEqualTo(2L);
        assertThat(vm.warehouseId()).isEqualTo(7L);
    }
}
