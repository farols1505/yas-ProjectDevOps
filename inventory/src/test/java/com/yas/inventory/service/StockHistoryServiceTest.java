package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockHistoryService stockHistoryService;

    private Warehouse warehouse;
    private Stock stock;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder().id(3L).name("W").addressId(1L).build();
        stock = Stock.builder().id(10L).productId(99L).quantity(5L).reservedQuantity(0L).warehouse(warehouse).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createStockHistories_buildsAndSaves() {
        List<StockQuantityVm> vms = List.of(new StockQuantityVm(10L, 2L, "note"));

        stockHistoryService.createStockHistories(List.of(stock), vms);

        ArgumentCaptor<List<StockHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        StockHistory h = captor.getValue().getFirst();
        assertThat(h.getProductId()).isEqualTo(99L);
        assertThat(h.getAdjustedQuantity()).isEqualTo(2L);
        assertThat(h.getNote()).isEqualTo("note");
        assertThat(h.getWarehouse()).isSameAs(warehouse);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createStockHistories_whenNoMatchingVm_thenSkips() {
        stockHistoryService.createStockHistories(List.of(stock), List.of(new StockQuantityVm(999L, 1L, "x")));

        ArgumentCaptor<List<StockHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void getStockHistories_mapsProductName() {
        StockHistory hist = StockHistory.builder()
            .id(1L)
            .productId(99L)
            .adjustedQuantity(-1L)
            .note("n")
            .warehouse(warehouse)
            .build();
        hist.setCreatedBy("user");
        hist.setCreatedOn(ZonedDateTime.parse("2024-01-01T10:00:00Z"));

        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(99L, 3L))
            .thenReturn(List.of(hist));
        when(productService.getProduct(99L)).thenReturn(new ProductInfoVm(99L, "Prod", "SKU", true));

        StockHistoryListVm list = stockHistoryService.getStockHistories(99L, 3L);

        assertThat(list.data()).hasSize(1);
        assertThat(list.data().getFirst().productName()).isEqualTo("Prod");
        assertThat(list.data().getFirst().adjustedQuantity()).isEqualTo(-1L);
    }
}
