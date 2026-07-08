package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TaxRateService.class)
public class TaxRateServiceTest {
    @MockitoBean
    TaxRateRepository taxRateRepository;
    @MockitoBean
    LocationService locationService;
    @MockitoBean
    TaxClassRepository taxClassRepository;

    @Autowired
    TaxRateService taxRateService;

    TaxClass taxClass;
    TaxRate taxRate;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder()
            .id(1L)
            .name("Value Added Tax")
            .build();
        taxRate = TaxRate.builder()
            .id(10L)
            .rate(8.5)
            .zipCode("70000")
            .taxClass(taxClass)
            .stateOrProvinceId(100L)
            .countryId(200L)
            .build();
        lenient().when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));
    }

    @Test
    void testFindAll_shouldReturnAllTaxRate() {
        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).hasSize(1).containsExactly(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void createTaxRate_shouldCreateWhenTaxClassExists() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(10.0, "12345", taxClass.getId(), 100L, 200L);
        when(taxClassRepository.existsById(taxClass.getId())).thenReturn(true);
        when(taxClassRepository.getReferenceById(taxClass.getId())).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate result = taxRateService.createTaxRate(taxRatePostVm);

        assertEquals(taxRatePostVm.rate(), result.getRate());
        assertEquals(taxRatePostVm.zipCode(), result.getZipCode());
        assertEquals(taxClass, result.getTaxClass());
        assertEquals(taxRatePostVm.stateOrProvinceId(), result.getStateOrProvinceId());
        assertEquals(taxRatePostVm.countryId(), result.getCountryId());
    }

    @Test
    void createTaxRate_shouldThrowNotFoundWhenTaxClassMissing() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(10.0, "12345", taxClass.getId(), 100L, 200L);
        when(taxClassRepository.existsById(taxClass.getId())).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.createTaxRate(taxRatePostVm));

        assertEquals(MessageCode.TAX_CLASS_NOT_FOUND, exception.getMessage());
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void updateTaxRate_shouldUpdateWhenDataExists() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(12.5, "54321", taxClass.getId(), 300L, 400L);
        when(taxRateRepository.findById(taxRate.getId())).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(taxClass.getId())).thenReturn(true);
        when(taxClassRepository.getReferenceById(taxClass.getId())).thenReturn(taxClass);

        taxRateService.updateTaxRate(taxRatePostVm, taxRate.getId());

        assertEquals(taxRatePostVm.rate(), taxRate.getRate());
        assertEquals(taxRatePostVm.zipCode(), taxRate.getZipCode());
        assertEquals(taxClass, taxRate.getTaxClass());
        assertEquals(taxRatePostVm.stateOrProvinceId(), taxRate.getStateOrProvinceId());
        assertEquals(taxRatePostVm.countryId(), taxRate.getCountryId());
        verify(taxRateRepository).save(taxRate);
    }

    @Test
    void updateTaxRate_shouldThrowNotFoundWhenTaxRateMissing() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(12.5, "54321", taxClass.getId(), 300L, 400L);
        when(taxRateRepository.findById(taxRate.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(taxRatePostVm, taxRate.getId()));

        assertEquals(MessageCode.TAX_RATE_NOT_FOUND, exception.getMessage());
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void updateTaxRate_shouldThrowNotFoundWhenTaxClassMissing() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(12.5, "54321", taxClass.getId(), 300L, 400L);
        when(taxRateRepository.findById(taxRate.getId())).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(taxClass.getId())).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(taxRatePostVm, taxRate.getId()));

        assertEquals(MessageCode.TAX_CLASS_NOT_FOUND, exception.getMessage());
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteWhenTaxRateExists() {
        when(taxRateRepository.existsById(taxRate.getId())).thenReturn(true);

        taxRateService.delete(taxRate.getId());

        verify(taxRateRepository).deleteById(taxRate.getId());
    }

    @Test
    void delete_shouldThrowNotFoundWhenTaxRateMissing() {
        when(taxRateRepository.existsById(taxRate.getId())).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.delete(taxRate.getId()));

        assertEquals(MessageCode.TAX_RATE_NOT_FOUND, exception.getMessage());
        verify(taxRateRepository, never()).deleteById(any());
    }

    @Test
    void findById_shouldReturnVmWhenTaxRateExists() {
        when(taxRateRepository.findById(taxRate.getId())).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(taxRate.getId());

        assertEquals(TaxRateVm.fromModel(taxRate), result);
    }

    @Test
    void findById_shouldThrowNotFoundWhenTaxRateMissing() {
        when(taxRateRepository.findById(taxRate.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.findById(taxRate.getId()));

        assertEquals(MessageCode.TAX_RATE_NOT_FOUND, exception.getMessage());
    }

    @Test
    void getPageableTaxRates_shouldMapPageMetadataAndLocationNames() {
        PageImpl<TaxRate> taxRatePage = new PageImpl<>(List.of(taxRate), PageRequest.of(0, 1), 1);
        when(taxRateRepository.findAll(PageRequest.of(0, 1))).thenReturn(taxRatePage);
        when(locationService.getStateOrProvinceAndCountryNames(List.of(taxRate.getStateOrProvinceId())))
            .thenReturn(List.of(new StateOrProvinceAndCountryGetNameVm(
                taxRate.getStateOrProvinceId(),
                "California",
                "United States"
            )));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 1);

        assertEquals(0, result.pageNo());
        assertEquals(1, result.pageSize());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(true, result.isLast());
        assertThat(result.taxRateGetDetailContent()).containsExactly(new TaxRateGetDetailVm(
            taxRate.getId(),
            taxRate.getRate(),
            taxRate.getZipCode(),
            taxClass.getName(),
            "California",
            "United States"
        ));
    }

    @Test
    void getTaxPercent_shouldReturnPercentWhenRepositoryReturnsValue() {
        when(taxRateRepository.getTaxPercent(200L, 100L, "70000", taxClass.getId())).thenReturn(12.5);

        double result = taxRateService.getTaxPercent(taxClass.getId(), 200L, 100L, "70000");

        assertEquals(12.5, result);
    }

    @Test
    void getTaxPercent_shouldReturnZeroWhenRepositoryReturnsNull() {
        when(taxRateRepository.getTaxPercent(200L, 100L, "70000", taxClass.getId())).thenReturn(null);

        double result = taxRateService.getTaxPercent(taxClass.getId(), 200L, 100L, "70000");

        assertEquals(0.0, result);
    }

    @Test
    void getBulkTaxRate_shouldReturnMappedTaxRates() {
        when(taxRateRepository.getBatchTaxRates(200L, 100L, "70000", new HashSet<>(List.of(taxClass.getId()))))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(taxClass.getId()), 200L, 100L, "70000");

        assertThat(result).containsExactly(TaxRateVm.fromModel(taxRate));
    }
}
