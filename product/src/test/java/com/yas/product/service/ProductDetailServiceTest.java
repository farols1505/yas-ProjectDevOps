package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    @Test
    void getProductDetailById_whenNotPublished_throwsNotFound() {
        Product draft = Product.builder().id(1L).isPublished(false).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(99L));
    }

    @Test
    void getProductDetailById_mapsBrandCategoriesAndMedia() {
        Brand brand = new Brand();
        brand.setId(5L);
        brand.setName("Acme");
        Category category = new Category();
        category.setId(2L);
        category.setName("Electronics");
        ProductCategory pc = ProductCategory.builder().category(category).build();
        ProductImage img = ProductImage.builder().id(10L).imageId(200L).build();
        Product product = Product.builder()
            .id(1L)
            .name("Laptop")
            .shortDescription("short")
            .description("long")
            .specification("spec")
            .sku("sku-1")
            .gtin("gtin-1")
            .slug("laptop")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(true)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .price(999.0)
            .metaTitle("mt")
            .metaKeyword("mk")
            .metaDescription("md")
            .taxClassId(3L)
            .thumbnailMediaId(50L)
            .brand(brand)
            .productCategories(List.of(pc))
            .productImages(List.of(img))
            .attributeValues(new ArrayList<>())
            .hasOptions(false)
            .products(new ArrayList<>())
            .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(50L)).thenReturn(new NoFileMediaVm(50L, "", "", "", "https://thumb"));
        when(mediaService.getMedia(200L)).thenReturn(new NoFileMediaVm(200L, "", "", "", "https://img"));

        var detail = productDetailService.getProductDetailById(1L);

        assertEquals(1L, detail.getId());
        assertEquals("Laptop", detail.getName());
        assertEquals(5L, detail.getBrandId());
        assertEquals("Acme", detail.getBrandName());
        assertEquals(1, detail.getCategories().size());
        assertEquals("Electronics", detail.getCategories().getFirst().getName());
        assertNotNull(detail.getThumbnail());
        assertEquals("https://thumb", detail.getThumbnail().url());
        assertEquals(1, detail.getProductImages().size());
        assertEquals("https://img", detail.getProductImages().getFirst().url());
        assertTrue(detail.getVariations().isEmpty());
    }

    @Test
    void getProductDetailById_whenHasOptions_includesPublishedVariations() {
        ProductOption option = new ProductOption();
        option.setId(7L);
        option.setName("Color");
        ProductOptionCombination combo = ProductOptionCombination.builder()
            .id(1L)
            .productOption(option)
            .value("Red")
            .displayOrder(0)
            .build();

        Product variation = Product.builder()
            .id(20L)
            .name("Laptop Red")
            .slug("laptop-red")
            .sku("sku-r")
            .gtin("g-r")
            .price(1000.0)
            .isPublished(true)
            .thumbnailMediaId(60L)
            .productImages(new ArrayList<>())
            .build();

        Product parent = Product.builder()
            .id(10L)
            .name("Laptop")
            .shortDescription("s")
            .description("d")
            .specification("sp")
            .sku("sku-p")
            .gtin("g-p")
            .slug("laptop")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(false)
            .price(999.0)
            .metaTitle("t")
            .metaKeyword("k")
            .metaDescription("m")
            .taxClassId(1L)
            .thumbnailMediaId(55L)
            .productCategories(new ArrayList<>())
            .productImages(new ArrayList<>())
            .attributeValues(new ArrayList<>())
            .hasOptions(true)
            .products(List.of(variation))
            .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia(55L)).thenReturn(new NoFileMediaVm(55L, "", "", "", "https://p"));
        when(mediaService.getMedia(60L)).thenReturn(new NoFileMediaVm(60L, "", "", "", "https://v"));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combo));

        var detail = productDetailService.getProductDetailById(10L);

        assertEquals(1, detail.getVariations().size());
        assertEquals(20L, detail.getVariations().getFirst().id());
        assertEquals(1, detail.getVariations().getFirst().options().size());
        assertEquals("Red", detail.getVariations().getFirst().options().get(7L));
    }

    @Test
    void getProductDetailById_mapsAttributeValues() {
        ProductAttributeGroup group = new ProductAttributeGroup();
        group.setId(1L);
        group.setName("General");
        ProductAttribute attr = new ProductAttribute();
        attr.setId(3L);
        attr.setName("Weight");
        attr.setProductAttributeGroup(group);
        ProductAttributeValue value = new ProductAttributeValue();
        value.setId(100L);
        value.setValue("2kg");
        value.setProductAttribute(attr);

        Product product = Product.builder()
            .id(1L)
            .name("Item")
            .shortDescription("s")
            .description("d")
            .specification("sp")
            .sku("sk")
            .gtin("gt")
            .slug("item")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .price(1.0)
            .metaTitle("t")
            .metaKeyword("k")
            .metaDescription("m")
            .taxClassId(1L)
            .attributeValues(List.of(value))
            .productCategories(new ArrayList<>())
            .productImages(new ArrayList<>())
            .hasOptions(false)
            .products(new ArrayList<>())
            .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var detail = productDetailService.getProductDetailById(1L);

        assertEquals(1, detail.getAttributeValues().size());
        assertEquals("Weight", detail.getAttributeValues().getFirst().nameProductAttribute());
        assertEquals("2kg", detail.getAttributeValues().getFirst().value());
    }
}
