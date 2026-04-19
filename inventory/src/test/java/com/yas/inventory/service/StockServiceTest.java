package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder().id(10L).name("W1").addressId(99L).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void addProductIntoWarehouse_whenNew_thenSavesStocks() {
        StockPostVm vm = new StockPostVm(1L, 10L);
        when(stockRepository.existsByWarehouseIdAndProductId(10L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "P1", "SKU1", false));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(vm));

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        List<Stock> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getProductId()).isEqualTo(1L);
        assertThat(saved.getFirst().getWarehouse()).isSameAs(warehouse);
        assertThat(saved.getFirst().getQuantity()).isZero();
    }

    @Test
    void addProductIntoWarehouse_whenStockExists_thenThrows() {
        StockPostVm vm = new StockPostVm(1L, 10L);
        when(stockRepository.existsByWarehouseIdAndProductId(10L, 1L)).thenReturn(true);

        assertThrows(
            StockExistingException.class,
            () -> stockService.addProductIntoWarehouse(List.of(vm)));
    }

    @Test
    void addProductIntoWarehouse_whenProductMissing_thenThrowsNotFound() {
        StockPostVm vm = new StockPostVm(1L, 10L);
        when(stockRepository.existsByWarehouseIdAndProductId(10L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(null);

        assertThrows(
            NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(List.of(vm)));
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseMissing_thenThrowsNotFound() {
        StockPostVm vm = new StockPostVm(1L, 10L);
        when(stockRepository.existsByWarehouseIdAndProductId(10L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "P1", "SKU1", false));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(List.of(vm)));
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_mapsProducts() {
        ProductInfoVm p = new ProductInfoVm(5L, "Name", "SKU", true);
        when(warehouseService.getProductWarehouse(10L, "Name", "SKU", FilterExistInWhSelection.YES))
            .thenReturn(List.of(p));

        Stock stock = Stock.builder()
            .id(100L)
            .productId(5L)
            .quantity(3L)
            .reservedQuantity(1L)
            .warehouse(warehouse)
            .build();
        when(stockRepository.findByWarehouseIdAndProductIdIn(eq(10L), eq(List.of(5L))))
            .thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(10L, "Name", "SKU");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(100L);
        assertThat(result.getFirst().productName()).isEqualTo("Name");
        assertThat(result.getFirst().quantity()).isEqualTo(3L);
        assertThat(result.getFirst().warehouseId()).isEqualTo(10L);
    }

    @Test
    void updateProductQuantityInStock_whenValid_thenUpdatesAndSyncsProduct() {
        Stock stock = Stock.builder()
            .id(1L)
            .productId(20L)
            .quantity(10L)
            .reservedQuantity(0L)
            .warehouse(warehouse)
            .build();
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        StockQuantityUpdateVm body = new StockQuantityUpdateVm(
            List.of(new StockQuantityVm(1L, 4L, "in")));

        stockService.updateProductQuantityInStock(body);

        assertThat(stock.getQuantity()).isEqualTo(14L);
        verify(stockRepository).saveAll(List.of(stock));
        verify(stockHistoryService).createStockHistories(anyList(), eq(body.stockQuantityList()));
        verify(productService).updateProductQuantity(anyList());
    }

    @Test
    void updateProductQuantityInStock_whenQuantityNull_thenTreatsAsZero() {
        Stock stock = Stock.builder()
            .id(1L)
            .productId(20L)
            .quantity(10L)
            .reservedQuantity(0L)
            .warehouse(warehouse)
            .build();
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(
            new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, null, "n"))));

        assertThat(stock.getQuantity()).isEqualTo(10L);
        verify(productService).updateProductQuantity(anyList());
    }

    @Test
    void updateProductQuantityInStock_whenExtraStockReturnedWithoutVm_thenSkipsThatStock() {
        Stock s1 = Stock.builder().id(1L).productId(1L).quantity(1L).reservedQuantity(0L).warehouse(warehouse).build();
        Stock s2 = Stock.builder().id(2L).productId(2L).quantity(2L).reservedQuantity(0L).warehouse(warehouse).build();
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(s1, s2));

        stockService.updateProductQuantityInStock(
            new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, 1L, "only first"))));

        assertThat(s1.getQuantity()).isEqualTo(2L);
        assertThat(s2.getQuantity()).isEqualTo(2L);
        verify(stockRepository).saveAll(anyList());
    }

    @Test
    void updateProductQuantityInStock_whenNegativeAdjustExceedsNegativeStock_thenBadRequest() {
        Stock stock = Stock.builder()
            .id(1L)
            .productId(1L)
            .quantity(-10L)
            .reservedQuantity(0L)
            .warehouse(warehouse)
            .build();
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        StockQuantityUpdateVm body = new StockQuantityUpdateVm(
            List.of(new StockQuantityVm(1L, -1L, "bad")));

        assertThrows(
            BadRequestException.class,
            () -> stockService.updateProductQuantityInStock(body));
    }

    @Test
    void updateProductQuantityInStock_whenNoStocks_thenDoesNotCallProductService() {
        when(stockRepository.findAllById(List.of(99L))).thenReturn(List.of());

        stockService.updateProductQuantityInStock(
            new StockQuantityUpdateVm(List.of(new StockQuantityVm(99L, 1L, "x"))));

        verify(productService, never()).updateProductQuantity(anyList());
    }
}
