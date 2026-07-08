package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.enumeration.DimensionUnit;
import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductExportingDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductVariationGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductCategoryRepository productCategoryRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private ProductOptionValueRepository productOptionValueRepository;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock
    private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getLatestProducts_whenCountNotPositive_returnsEmptyList() {
        assertTrue(productService.getLatestProducts(0).isEmpty());
        assertTrue(productService.getLatestProducts(-1).isEmpty());
    }

    @Test
    void getLatestProducts_mapsRepositoryResults() {
        Product p = Product.builder().id(1L).name("A").slug("a").build();
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of(p));

        List<ProductListVm> result = productService.getLatestProducts(5);

        assertEquals(1, result.size());
        assertEquals("A", result.getFirst().name());
        verify(productRepository).getLatestProducts(PageRequest.of(0, 5));
    }

    @Test
    void getLatestProducts_whenRepositoryEmpty_returnsEmptyList() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of());

        assertTrue(productService.getLatestProducts(3).isEmpty());
    }

    @Test
    void getProductsWithFilter_returnsPagedViewModel() {
        Product p = Product.builder().id(2L).name("Phone").slug("phone").build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.getProductsWithFilter(eq("phone"), eq("BrandX"), any(Pageable.class)))
            .thenReturn(page);

        ProductListGetVm vm = productService.getProductsWithFilter(0, 10, "Phone", "BrandX");

        assertEquals(1, vm.productContent().size());
        assertEquals(0, vm.pageNo());
        assertEquals(10, vm.pageSize());
        assertEquals(1, vm.totalElements());
    }

    @Test
    void getProductById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getProductById_buildsDetailWithMediaUrls() {
        ProductImage img = ProductImage.builder().id(1L).imageId(10L).build();
        Product product = Product.builder()
            .id(1L)
            .name("Item")
            .thumbnailMediaId(5L)
            .productImages(List.of(img))
            .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(10L)).thenReturn(new NoFileMediaVm(10L, "", "", "", "http://img"));
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", "http://thumb"));

        var detail = productService.getProductById(1L);

        assertEquals("Item", detail.name());
        assertNotNull(detail.thumbnailMedia());
        assertEquals("http://thumb", detail.thumbnailMedia().url());
        assertEquals(1, detail.productImageMedias().size());
        assertEquals("http://img", detail.productImageMedias().getFirst().url());
    }

    @Test
    void updateMainProductFromVm_updatesScalarFields() {
        Product product = Product.builder().id(3L).name("Old").slug("old").build();
        ProductPutVm putVm = new ProductPutVm(
            "New name",
            "NEW-SLUG",
            199.0,
            true,
            true,
            false,
            true,
            true,
            7L,
            List.of(),
            "short",
            "long",
            "spec",
            "sku-1",
            "gtin-1",
            1.0,
            DimensionUnit.CM,
            2.0,
            1.0,
            3.0,
            "metaTitle",
            "metaKw",
            "metaDesc",
            9L,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            11L
        );

        productService.updateMainProductFromVm(putVm, product);

        assertEquals("New name", product.getName());
        assertEquals("new-slug", product.getSlug());
        assertEquals(199.0, product.getPrice());
        assertEquals("sku-1", product.getSku());
        assertEquals(11L, product.getTaxClassId());
    }

    @Test
    void setProductImages_whenIdsEmpty_deletesExistingAndReturnsEmpty() {
        Product product = Product.builder().id(4L).build();

        List<ProductImage> result = productService.setProductImages(List.of(), product);

        assertTrue(result.isEmpty());
        verify(productImageRepository).deleteByProductId(4L);
    }

    @Test
    void setProductImages_whenNoExistingImages_buildsAllNew() {
        Product product = Product.builder().id(5L).productImages(null).build();

        List<ProductImage> result = productService.setProductImages(List.of(100L, 101L), product);

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getImageId());
        assertEquals(101L, result.get(1).getImageId());
        assertEquals(product, result.get(0).getProduct());
    }

    @Test
    void getProductSlug_whenProductHasParent_returnsParentSlugAndVariationId() {
        Product parent = Product.builder().id(10L).slug("parent-slug").build();
        Product child = Product.builder().id(20L).slug("child-slug").parent(parent).build();
        when(productRepository.findById(20L)).thenReturn(Optional.of(child));

        ProductSlugGetVm vm = productService.getProductSlug(20L);

        assertEquals("parent-slug", vm.slug());
        assertEquals(20L, vm.productVariantId());
    }

    @Test
    void getProductSlug_whenMainProduct_returnsOwnSlug() {
        Product main = Product.builder().id(30L).slug("main-slug").build();
        when(productRepository.findById(30L)).thenReturn(Optional.of(main));

        ProductSlugGetVm vm = productService.getProductSlug(30L);

        assertEquals("main-slug", vm.slug());
        assertEquals(null, vm.productVariantId());
    }

    @Test
    void getProductEsDetailById_mapsCoreFields() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("B");
        Product product = Product.builder()
            .id(40L)
            .name("EsProduct")
            .slug("es-product")
            .price(12.5)
            .isPublished(true)
            .isVisibleIndividually(true)
            .isAllowedToOrder(true)
            .isFeatured(false)
            .thumbnailMediaId(3L)
            .brand(brand)
            .productCategories(new ArrayList<>())
            .attributeValues(new ArrayList<>())
            .build();
        when(productRepository.findById(40L)).thenReturn(Optional.of(product));

        ProductEsDetailVm vm = productService.getProductEsDetailById(40L);

        assertEquals(40L, vm.id());
        assertEquals("EsProduct", vm.name());
        assertEquals(3L, vm.thumbnailMediaId());
        assertEquals("B", vm.brand());
    }

    @Test
    void subtractStockQuantity_mergesLinesForSameProduct_andClampsToZero() {
        Product product = Product.builder().id(1L).stockQuantity(5L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productService.subtractStockQuantity(List.of(
            new ProductQuantityPutVm(1L, 3L),
            new ProductQuantityPutVm(1L, 5L)
        ));

        assertEquals(0L, product.getStockQuantity());
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void restoreStockQuantity_addsQuantitiesForSameProduct() {
        Product product = Product.builder().id(2L).stockQuantity(10L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productService.restoreStockQuantity(List.of(
            new ProductQuantityPutVm(2L, 4L),
            new ProductQuantityPutVm(2L, 1L)
        ));

        assertEquals(15L, product.getStockQuantity());
    }

    @Test
    void updateProductQuantity_setsStockFromPostVm() {
        Product p = Product.builder().id(3L).stockQuantity(0L).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(p));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productService.updateProductQuantity(List.of(new ProductQuantityPostVm(3L, 100L)));

        assertEquals(100L, p.getStockQuantity());
    }

    @Test
    void getProductCheckoutList_enrichesThumbnailUrlWhenPresent() {
        Brand brand = new Brand();
        brand.setId(1L);
        Product p = Product.builder()
            .id(5L)
            .name("C")
            .thumbnailMediaId(8L)
            .brand(brand)
            .price(1.0)
            .taxClassId(1L)
            .build();
        Page<Product> page = new PageImpl<>(List.of(p));
        when(productRepository.findAllPublishedProductsByIds(eq(List.of(5L)), any(Pageable.class)))
            .thenReturn(page);
        when(mediaService.getMedia(8L)).thenReturn(new NoFileMediaVm(8L, "", "", "", "https://cdn/x.png"));

        var vm = productService.getProductCheckoutList(0, 10, List.of(5L));

        assertEquals(1, vm.productCheckoutListVms().size());
        assertEquals("https://cdn/x.png", vm.productCheckoutListVms().getFirst().thumbnailUrl());
    }

    @Test
    void getProductsForWarehouse_delegatesToRepository() {
        Product p = Product.builder().id(9L).name("W").sku("S").build();
        when(productRepository.findProductForWarehouse("n", "s", List.of(1L), FilterExistInWhSelection.ALL.name()))
            .thenReturn(List.of(p));

        var list = productService.getProductsForWarehouse("n", "s", List.of(1L), FilterExistInWhSelection.ALL);

        assertEquals(1, list.size());
        assertEquals(9L, list.getFirst().id());
        assertEquals("S", list.getFirst().sku());
    }

    @Test
    void getProductByIds_mapsEachProduct() {
        Product a = Product.builder().id(1L).name("A").slug("a").build();
        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(a));

        List<ProductListVm> vms = productService.getProductByIds(List.of(1L, 2L));

        assertEquals(1, vms.size());
        assertEquals("A", vms.getFirst().name());
    }

    @Test
    void deleteProduct_whenVariationWithCombinations_deletesCombinationsBeforeSave() {
        Product parent = Product.builder().id(100L).build();
        Product variation = Product.builder()
            .id(200L)
            .isPublished(true)
            .parent(parent)
            .build();
        ProductOption option = new ProductOption();
        option.setId(1L);
        ProductOptionCombination combo = ProductOptionCombination.builder()
            .id(1L)
            .product(variation)
            .productOption(option)
            .value("v")
            .displayOrder(0)
            .build();
        when(productRepository.findById(200L)).thenReturn(Optional.of(variation));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combo));

        productService.deleteProduct(200L);

        assertFalse(variation.isPublished());
        verify(productOptionCombinationRepository).deleteAll(List.of(combo));
        verify(productRepository).save(variation);
    }

    @Test
    void deleteProduct_whenMainProduct_skipsCombinationCleanup() {
        Product main = Product.builder().id(300L).isPublished(true).parent(null).build();
        when(productRepository.findById(300L)).thenReturn(Optional.of(main));

        productService.deleteProduct(300L);

        assertFalse(main.isPublished());
        verify(productOptionCombinationRepository, never()).findAllByProduct(any());
        verify(productRepository).save(main);
    }

    @Test
    void getRelatedProductsBackoffice_mapsRelatedProducts() {
        Product related = Product.builder()
            .id(2L)
            .name("R")
            .slug("r")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .price(9.0)
            .taxClassId(1L)
            .parent(null)
            .build();
        Product main = Product.builder().id(1L).relatedProducts(new ArrayList<>()).build();
        ProductRelated pr = ProductRelated.builder().product(main).relatedProduct(related).build();
        main.getRelatedProducts().add(pr);
        when(productRepository.findById(1L)).thenReturn(Optional.of(main));

        List<ProductListVm> vms = productService.getRelatedProductsBackoffice(1L);

        assertEquals(1, vms.size());
        assertEquals(2L, vms.getFirst().id());
        assertEquals("R", vms.getFirst().name());
    }

    @Test
    void createProduct_whenNoVariations_persistsMainProductAndReturnsVm() {
        Brand brand = new Brand();
        brand.setId(1L);
        Category category = new Category();
        category.setId(1L);
        category.setName("Cat");
        ProductPostVm postVm = new ProductPostVm(
            "Widget",
            "widget-slug",
            1L,
            List.of(1L),
            "short",
            "desc",
            "spec",
            "w-sku",
            "w-gtin",
            1.0,
            DimensionUnit.CM,
            10.0,
            5.0,
            5.0,
            99.0,
            true,
            true,
            false,
            true,
            true,
            "mt",
            "mk",
            "md",
            2L,
            null,
            List.of(),
            List.of(),
            null,
            null,
            3L
        );
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findAllById(List.of(1L))).thenReturn(List.of(category));
        when(productRepository.findBySlugAndIsPublishedTrue("widget-slug")).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("w-sku")).thenReturn(Optional.empty());
        when(productRepository.findByGtinAndIsPublishedTrue("w-gtin")).thenReturn(Optional.empty());
        when(productRepository.findAllById(anyList())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(500L);
            return p;
        });

        ProductGetDetailVm result = productService.createProduct(postVm);

        assertEquals(500L, result.id());
        assertEquals("Widget", result.name());
        assertEquals("widget-slug", result.slug());
        verify(productCategoryRepository).saveAll(anyList());
        verify(productImageRepository).saveAll(anyList());
    }

    @Test
    void createProduct_whenLengthLessThanWidth_throwsBadRequest() {
        ProductPostVm postVm = new ProductPostVm(
            "Bad",
            "bad",
            null,
            List.of(),
            "s",
            "d",
            "sp",
            "sku",
            "",
            1.0,
            DimensionUnit.CM,
            1.0,
            5.0,
            5.0,
            1.0,
            true,
            true,
            true,
            true,
            true,
            "m1",
            "m2",
            "m3",
            null,
            null,
            List.of(),
            List.of(),
            null,
            null,
            1L
        );

        assertThrows(BadRequestException.class, () -> productService.createProduct(postVm));
    }

    @Test
    void getProductsByBrand_returnsThumbnails() {
        Brand brand = new Brand();
        brand.setId(2L);
        brand.setSlug("acme");
        Product p = Product.builder().id(1L).name("P").slug("p").thumbnailMediaId(9L).build();
        when(brandRepository.findBySlug("acme")).thenReturn(Optional.of(brand));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(p));
        when(mediaService.getMedia(9L)).thenReturn(new NoFileMediaVm(9L, "", "", "", "https://t"));

        List<ProductThumbnailVm> list = productService.getProductsByBrand("acme");

        assertEquals(1, list.size());
        assertEquals("https://t", list.getFirst().thumbnailUrl());
    }

    @Test
    void getProductsByBrand_whenBrandMissing_throwsNotFound() {
        when(brandRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsByBrand("unknown"));
    }

    @Test
    void getProductsFromCategory_returnsPagedThumbnails() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setSlug("phones");
        Product p = Product.builder().id(3L).name("Phone").slug("ph").thumbnailMediaId(4L).build();
        ProductCategory pc = ProductCategory.builder().product(p).category(cat).build();
        Page<ProductCategory> page = new PageImpl<>(List.of(pc), PageRequest.of(0, 5), 1);
        when(categoryRepository.findBySlug("phones")).thenReturn(Optional.of(cat));
        when(productCategoryRepository.findAllByCategory(any(Pageable.class), eq(cat))).thenReturn(page);
        when(mediaService.getMedia(4L)).thenReturn(new NoFileMediaVm(4L, "", "", "", "https://img"));

        var vm = productService.getProductsFromCategory(0, 5, "phones");

        assertEquals(1, vm.productContent().size());
        assertEquals(1, vm.totalElements());
    }

    @Test
    void getListFeaturedProducts_mapsPage() {
        Product p = Product.builder().id(1L).name("F").slug("f").thumbnailMediaId(2L).price(10.0).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.getFeaturedProduct(any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(2L)).thenReturn(new NoFileMediaVm(2L, "", "", "", "u"));

        ProductFeatureGetVm vm = productService.getListFeaturedProducts(0, 10);

        assertEquals(1, vm.productList().size());
        assertEquals(1, vm.totalPage());
    }

    @Test
    void getFeaturedProductsById_whenChildHasNoThumbnail_usesParentMedia() {
        Product parent = Product.builder().id(10L).thumbnailMediaId(99L).build();
        Product child = Product.builder().id(20L).name("C").slug("c").price(1.0).thumbnailMediaId(5L).parent(parent).build();
        when(productRepository.findAllByIdIn(List.of(20L))).thenReturn(List.of(child));
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", ""));
        when(productRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia(99L)).thenReturn(new NoFileMediaVm(99L, "", "", "", "https://parent"));

        List<ProductThumbnailGetVm> list = productService.getFeaturedProductsById(List.of(20L));

        assertEquals(1, list.size());
        assertEquals("https://parent", list.getFirst().thumbnailUrl());
    }

    @Test
    void getProductsByMultiQuery_returnsPagedResults() {
        Product p = Product.builder().id(1L).name("X").slug("x").thumbnailMediaId(3L).price(5.0).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 4), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
            eq("x"), eq("c"), eq(1.0), eq(10.0), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(3L)).thenReturn(new NoFileMediaVm(3L, "", "", "", "u"));

        ProductsGetVm vm = productService.getProductsByMultiQuery(0, 4, "X", "c", 1.0, 10.0);

        assertEquals(1, vm.productContent().size());
        assertEquals(0, vm.pageNo());
    }

    @Test
    void getProductDetail_bySlug_returnsStorefrontDetail() {
        Category cat = new Category();
        cat.setName("Books");
        ProductCategory pc = ProductCategory.builder().category(cat).build();
        Brand brand = new Brand();
        brand.setName("Bnd");
        Product product = Product.builder()
            .id(7L)
            .name("Book")
            .slug("book")
            .shortDescription("s")
            .description("d")
            .specification("sp")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .hasOptions(false)
            .price(12.0)
            .brand(brand)
            .productCategories(List.of(pc))
            .productImages(List.of())
            .attributeValues(new ArrayList<>())
            .thumbnailMediaId(8L)
            .build();
        when(productRepository.findBySlugAndIsPublishedTrue("book")).thenReturn(Optional.of(product));
        when(mediaService.getMedia(8L)).thenReturn(new NoFileMediaVm(8L, "", "", "", "https://thumb"));

        ProductDetailGetVm vm = productService.getProductDetail("book");

        assertEquals(7L, vm.id());
        assertEquals("Book", vm.name());
        assertEquals("https://thumb", vm.thumbnailMediaUrl());
        assertEquals("Bnd", vm.brandName());
        assertEquals(1, vm.productCategories().size());
    }

    @Test
    void getProductDetail_whenSlugMissing_throwsNotFound() {
        when(productRepository.findBySlugAndIsPublishedTrue("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductDetail("nope"));
    }

    @Test
    void getProductVariationsByParentId_whenHasOptions_returnsVariations() {
        Product parent = Product.builder().id(1L).hasOptions(true).products(new ArrayList<>()).build();
        Product child = Product.builder()
            .id(2L)
            .name("V")
            .slug("v")
            .sku("sk")
            .gtin("gt")
            .price(2.0)
            .isPublished(true)
            .thumbnailMediaId(3L)
            .productImages(new ArrayList<>())
            .build();
        parent.getProducts().add(child);
        ProductOption opt = new ProductOption();
        opt.setId(9L);
        ProductOptionCombination combo = ProductOptionCombination.builder()
            .productOption(opt)
            .value("Red")
            .displayOrder(0)
            .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(productOptionCombinationRepository.findAllByProduct(child)).thenReturn(List.of(combo));
        when(mediaService.getMedia(3L)).thenReturn(new NoFileMediaVm(3L, "", "", "", "tu"));

        List<ProductVariationGetVm> list = productService.getProductVariationsByParentId(1L);

        assertEquals(1, list.size());
        assertEquals(2L, list.getFirst().id());
        assertEquals("Red", list.getFirst().options().get(9L));
    }

    @Test
    void getProductVariationsByParentId_whenNoOptions_returnsEmpty() {
        Product parent = Product.builder().id(1L).hasOptions(false).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertTrue(productService.getProductVariationsByParentId(1L).isEmpty());
    }

    @Test
    void exportProducts_mapsRows() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Bn");
        Product p = Product.builder()
            .id(1L)
            .name("E")
            .shortDescription("s")
            .description("d")
            .specification("sp")
            .sku("sk")
            .gtin("g")
            .slug("e")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .price(1.0)
            .brand(brand)
            .metaTitle("t")
            .metaKeyword("k")
            .metaDescription("m")
            .build();
        when(productRepository.getExportingProducts("e", "Bn")).thenReturn(List.of(p));

        List<ProductExportingDetailVm> rows = productService.exportProducts("E", "Bn");

        assertEquals(1, rows.size());
        assertEquals(1L, rows.getFirst().id());
        assertEquals("Bn", rows.getFirst().brandName());
    }

    @Test
    void getRelatedProductsStorefront_filtersPublishedOnly() {
        Product main = Product.builder().id(1L).build();
        Product relPub = Product.builder().id(2L).name("A").slug("a").isPublished(true).thumbnailMediaId(3L).price(1.0).build();
        Product relDraft = Product.builder().id(3L).name("B").slug("b").isPublished(false).thumbnailMediaId(4L).price(2.0).build();
        ProductRelated pr1 = ProductRelated.builder().product(main).relatedProduct(relPub).build();
        ProductRelated pr2 = ProductRelated.builder().product(main).relatedProduct(relDraft).build();
        Page<ProductRelated> page = new PageImpl<>(List.of(pr1, pr2), PageRequest.of(0, 10), 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(main));
        when(productRelatedRepository.findAllByProduct(eq(main), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(3L)).thenReturn(new NoFileMediaVm(3L, "", "", "", "u"));

        ProductsGetVm vm = productService.getRelatedProductsStorefront(1L, 0, 10);

        assertEquals(1, vm.productContent().size());
        assertEquals(2L, vm.productContent().getFirst().id());
    }

    @Test
    void getProductByCategoryIds_and_getProductByBrandIds_delegateToRepository() {
        Product p1 = Product.builder().id(1L).name("A").slug("a").build();
        when(productRepository.findByCategoryIdsIn(List.of(9L))).thenReturn(List.of(p1));
        when(productRepository.findByBrandIdsIn(List.of(8L))).thenReturn(List.of(p1));

        assertEquals(1, productService.getProductByCategoryIds(List.of(9L)).size());
        assertEquals(1, productService.getProductByBrandIds(List.of(8L)).size());
    }

    @Test
    void getProductCheckoutList_whenThumbnailUrlEmpty_keepsVmUnchanged() {
        Brand brand = new Brand();
        brand.setId(1L);
        Product p = Product.builder()
            .id(1L)
            .name("N")
            .thumbnailMediaId(2L)
            .brand(brand)
            .price(1.0)
            .taxClassId(1L)
            .build();
        Page<Product> page = new PageImpl<>(List.of(p));
        when(productRepository.findAllPublishedProductsByIds(eq(List.of(1L)), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(2L)).thenReturn(new NoFileMediaVm(2L, "", "", "", ""));

        var vm = productService.getProductCheckoutList(0, 5, List.of(1L));

        assertEquals("", vm.productCheckoutListVms().getFirst().thumbnailUrl());
    }

    @Test
    void subtractStockQuantity_whenStockTrackingDisabled_leavesQuantityUnchanged() {
        Product product = Product.builder().id(1L).stockQuantity(10L).stockTrackingEnabled(false).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productService.subtractStockQuantity(List.of(new ProductQuantityPutVm(1L, 5L)));

        assertEquals(10L, product.getStockQuantity());
    }

    @Test
    void setProductImages_whenExistingImages_addsNewAndDeletesRemoved() {
        Product product = Product.builder().id(20L).build();
        ProductImage img1 = ProductImage.builder().id(1L).imageId(1L).product(product).build();
        ProductImage img2 = ProductImage.builder().id(2L).imageId(2L).product(product).build();
        product.setProductImages(new ArrayList<>(List.of(img1, img2)));

        List<ProductImage> toSave = productService.setProductImages(List.of(2L, 3L), product);

        assertEquals(1, toSave.size());
        assertEquals(3L, toSave.getFirst().getImageId());
        verify(productImageRepository).deleteByImageIdInAndProductId(eq(List.of(1L)), eq(20L));
    }
}
