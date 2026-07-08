package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TaxClassService.class)
class TaxClassServiceTest {
    @MockitoBean
    TaxClassRepository taxClassRepository;

    @Autowired
    TaxClassService taxClassService;

    @Test
    void findAllTaxClasses_shouldReturnSortedTaxClasses() {
        TaxClass taxClassA = TaxClass.builder().id(1L).name("A Tax").build();
        TaxClass taxClassB = TaxClass.builder().id(2L).name("B Tax").build();
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
            .thenReturn(List.of(taxClassA, taxClassB));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).containsExactly(TaxClassVm.fromModel(taxClassA), TaxClassVm.fromModel(taxClassB));
        verify(taxClassRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    void findById_shouldReturnTaxClassWhenExists() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertEquals(TaxClassVm.fromModel(taxClass), result);
    }

    @Test
    void findById_shouldThrowNotFoundWhenMissing() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxClassService.findById(1L));

        assertEquals(MessageCode.TAX_CLASS_NOT_FOUND, exception.getMessage());
    }

    @Test
    void create_shouldSaveNewTaxClassWhenNameIsUnique() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("ignored", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxClass result = taxClassService.create(taxClassPostVm);

        assertEquals("VAT", result.getName());
        verify(taxClassRepository).save(result);
    }

    @Test
    void create_shouldThrowDuplicatedWhenNameAlreadyExists() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("ignored", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> taxClassService.create(taxClassPostVm));

        assertEquals(MessageCode.NAME_ALREADY_EXITED, exception.getMessage());
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateTaxClassWhenNameIsUnique() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("Old Name").build();
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("ignored", "New Name");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New Name", 1L)).thenReturn(false);

        taxClassService.update(taxClassPostVm, 1L);

        assertEquals("New Name", taxClass.getName());
        verify(taxClassRepository).save(taxClass);
    }

    @Test
    void update_shouldThrowNotFoundWhenTaxClassMissing() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("ignored", "New Name");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxClassService.update(taxClassPostVm, 1L));

        assertEquals(MessageCode.TAX_CLASS_NOT_FOUND, exception.getMessage());
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowDuplicatedWhenNameAlreadyUsedByOtherTaxClass() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("Old Name").build();
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("ignored", "New Name");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New Name", 1L)).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> taxClassService.update(taxClassPostVm, 1L));

        assertEquals(MessageCode.NAME_ALREADY_EXITED, exception.getMessage());
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteWhenTaxClassExists() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundWhenTaxClassMissing() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxClassService.delete(1L));

        assertEquals(MessageCode.TAX_CLASS_NOT_FOUND, exception.getMessage());
        verify(taxClassRepository, never()).deleteById(any());
    }

    @Test
    void getPageableTaxClasses_shouldReturnPageableResult() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        PageImpl<TaxClass> page = new PageImpl<>(List.of(taxClass), PageRequest.of(0, 1), 1);
        when(taxClassRepository.findAll(PageRequest.of(0, 1))).thenReturn(page);

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 1);

        assertEquals(0, result.pageNo());
        assertEquals(1, result.pageSize());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(true, result.isLast());
        assertThat(result.taxClassContent()).containsExactly(TaxClassVm.fromModel(taxClass));
    }
}