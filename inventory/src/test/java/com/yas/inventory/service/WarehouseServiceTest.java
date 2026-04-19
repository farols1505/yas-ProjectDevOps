package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder().id(1L).name("Main").addressId(100L).build();
    }

    @Test
    void findAllWarehouses_returnsVms() {
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));

        List<WarehouseGetVm> result = warehouseService.findAllWarehouses();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Main");
    }

    @Test
    void getProductWarehouse_whenWarehouseHasProducts_thenMarksExistFlag() {
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of(10L));
        ProductInfoVm fromProduct = new ProductInfoVm(10L, "A", "S", false);
        when(productService.filterProducts("a", "b", List.of(10L), FilterExistInWhSelection.YES))
            .thenReturn(List.of(fromProduct));

        List<ProductInfoVm> result =
            warehouseService.getProductWarehouse(1L, "a", "b", FilterExistInWhSelection.YES);

        assertThat(result.getFirst().existInWh()).isTrue();
    }

    @Test
    void getProductWarehouse_whenNoStockInWarehouse_thenReturnsFilterAsIs() {
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of());
        ProductInfoVm p = new ProductInfoVm(1L, "A", "S", false);
        when(productService.filterProducts("a", "b", List.of(), FilterExistInWhSelection.ALL))
            .thenReturn(List.of(p));

        List<ProductInfoVm> result =
            warehouseService.getProductWarehouse(1L, "a", "b", FilterExistInWhSelection.ALL);

        assertThat(result).containsExactly(p);
    }

    @Test
    void findById_whenFound_thenReturnsDetail() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        AddressDetailVm addr = AddressDetailVm.builder()
            .id(100L)
            .contactName("c")
            .phone("p")
            .addressLine1("l1")
            .addressLine2("l2")
            .city("city")
            .zipCode("z")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
        when(locationService.getAddressById(100L)).thenReturn(addr);

        WarehouseDetailVm detail = warehouseService.findById(1L);

        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.name()).isEqualTo("Main");
        assertThat(detail.contactName()).isEqualTo("c");
    }

    @Test
    void findById_whenMissing_thenThrows() {
        when(warehouseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.findById(9L));
    }

    @Test
    void create_whenNameDuplicated_thenThrows() {
        WarehousePostVm vm = samplePostVm();
        when(warehouseRepository.existsByName(vm.name())).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.create(vm));
    }

    @Test
    void create_whenValid_thenSavesWarehouse() {
        WarehousePostVm vm = samplePostVm();
        when(warehouseRepository.existsByName(vm.name())).thenReturn(false);
        when(locationService.createAddress(any(AddressPostVm.class)))
            .thenReturn(AddressVm.builder().id(200L).contactName("c").phone("p")
                .addressLine1("l1").city("city").zipCode("z")
                .districtId(1L).stateOrProvinceId(2L).countryId(3L).build());
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            w.setId(55L);
            return w;
        });

        Warehouse created = warehouseService.create(vm);

        assertThat(created.getId()).isEqualTo(55L);
        assertThat(created.getAddressId()).isEqualTo(200L);
        verify(locationService).createAddress(any(AddressPostVm.class));
    }

    @Test
    void update_whenWarehouseMissing_thenThrows() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> warehouseService.update(samplePostVm(), 1L));
    }

    @Test
    void update_whenNameUsedByOther_thenThrows() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        WarehousePostVm vm = samplePostVm();
        when(warehouseRepository.existsByNameWithDifferentId(vm.name(), 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.update(vm, 1L));
    }

    @Test
    void update_whenValid_thenUpdatesAddressAndSaves() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        WarehousePostVm vm = samplePostVm();
        when(warehouseRepository.existsByNameWithDifferentId(vm.name(), 1L)).thenReturn(false);

        warehouseService.update(vm, 1L);

        verify(locationService).updateAddress(eq(100L), any(AddressPostVm.class));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void delete_whenMissing_thenThrows() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.delete(1L));
    }

    @Test
    void delete_whenFound_thenDeletesWarehouseAndAddress() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        warehouseService.delete(1L);

        verify(warehouseRepository).deleteById(1L);
        verify(locationService).deleteAddress(100L);
    }

    @Test
    void getPageableWarehouses_returnsPageMetadata() {
        when(warehouseRepository.findAll(PageRequest.of(0, 5)))
            .thenReturn(new PageImpl<>(List.of(warehouse), PageRequest.of(0, 5), 1));

        WarehouseListGetVm page = warehouseService.getPageableWarehouses(0, 5);

        assertThat(page.pageNo()).isZero();
        assertThat(page.pageSize()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.warehouseContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
    }

    private static WarehousePostVm samplePostVm() {
        return WarehousePostVm.builder()
            .name("Main")
            .contactName("c")
            .phone("12345678")
            .addressLine1("l1")
            .addressLine2("l2")
            .city("city")
            .zipCode("zip")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
    }
}
