package ru.nu1ts.recipebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.dto.*;
import ru.nu1ts.recipebook.exception.BusinessException;
import ru.nu1ts.recipebook.exception.ErrorCode;
import ru.nu1ts.recipebook.exception.ProductDeleteConflictException;
import ru.nu1ts.recipebook.exception.ResourceNotFoundException;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.entity.ProductPhoto;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishIngredientRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.repository.specification.ProductSpecification;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ProductListItem> getProducts(
            String search,
            ProductCategory category,
            CookingRequired cookingRequired,
            List<ProductFlag> flags,
            String sortBy,
            String sortOrder
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Specification<Product> spec = ProductSpecification.filter(search, category, cookingRequired, flags);

        return productRepository.findAll(spec, sort).stream()
                .map(this::mapToListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        return mapToDto(product);
    }

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        validateNutrition(request.getProteins(), request.getFats(), request.getCarbohydrates());
        Product product = Product.builder()
                .name(request.getName())
                .calories(request.getCalories())
                .proteins(request.getProteins())
                .fats(request.getFats())
                .carbohydrates(request.getCarbohydrates())
                .composition(request.getComposition())
                .category(request.getCategory())
                .cookingRequired(request.getCookingRequired())
                .flags(request.getFlags() != null ? request.getFlags() : new ArrayList<>())
                .build();

        List<MultipartFile> validFiles = fileStorageService.filterValidFiles(request.getPhotos());
        if (!validFiles.isEmpty()) {
            savePhotos(product, validFiles);
        }

        return mapToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(UUID productId, ProductUpdateRequest request) {
        validateNutrition(request.getProteins(), request.getFats(), request.getCarbohydrates());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        product.setName(request.getName());
        product.setCalories(request.getCalories());
        product.setProteins(request.getProteins());
        product.setFats(request.getFats());
        product.setCarbohydrates(request.getCarbohydrates());
        product.setComposition(request.getComposition());
        product.setCategory(request.getCategory());
        product.setCookingRequired(request.getCookingRequired());
        product.setFlags(request.getFlags() != null ? request.getFlags() : new ArrayList<>());

        List<String> keepUrls = request.getPhotosToKeep() != null ? request.getPhotosToKeep() : new ArrayList<>();
        updatePhotos(product, keepUrls, request.getPhotos());

        return mapToDto(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        List<Object[]> dishData = dishIngredientRepository.findDishRefsByProductId(productId);
        if (!dishData.isEmpty()) {
            List<DishReference> references = dishData.stream()
                    .map(data -> DishReference.builder()
                            .id(data[0].toString())
                            .name(data[1].toString())
                            .build())
                    .toList();
            throw new ProductDeleteConflictException(references);
        }

        List<String> urls = product.getPhotos().stream()
                .map(ProductPhoto::getPhotoUrl)
                .toList();
        fileStorageService.deleteFiles(urls);

        productRepository.delete(product);
    }

    private void updatePhotos(Product product, List<String> keepUrls, List<MultipartFile> newFiles) {
        fileStorageService.deleteUnusedFiles(product.getPhotos().stream().map(ProductPhoto::getPhotoUrl).toList(), keepUrls);
        product.getPhotos().removeIf(photo -> !keepUrls.contains(photo.getPhotoUrl()));

        List<MultipartFile> validNewFiles = fileStorageService.filterValidFiles(newFiles);
        if (!validNewFiles.isEmpty()) {
            List<UploadedFile> uploadedFiles = fileStorageService.saveFiles(validNewFiles);
            for (UploadedFile uploaded : uploadedFiles) {
                ProductPhoto photo = ProductPhoto.builder()
                        .photoUrl(uploaded.getUrl())
                        .product(product)
                        .build();
                product.addPhoto(photo);
            }
        }

        for (int i = 0; i < product.getPhotos().size(); i++) {
            product.getPhotos().get(i).setSortOrder(i);
        }
    }

    private void savePhotos(Product product, List<MultipartFile> files) {
        List<UploadedFile> uploadedFiles = fileStorageService.saveFiles(files);
        for (int i = 0; i < uploadedFiles.size(); i++) {
            ProductPhoto photo = ProductPhoto.builder()
                    .photoUrl(uploadedFiles.get(i).getUrl())
                    .sortOrder(i)
                    .build();
            product.addPhoto(photo);
        }
    }

    private void validateNutrition(Double p, Double f, Double c) {
        if (p + f + c > 100.0) {
            throw new BusinessException(ErrorCode.BJU_SUM_EXCEEDED, "The amount of BJU per 100 grams cannot exceed 100");
        }
    }

    private ProductListItem mapToListItem(Product product) {
        String mainPhoto = product.getPhotos().isEmpty() ? null : product.getPhotos().get(0).getPhotoUrl();

        return ProductListItem.builder()
                .id(product.getId())
                .name(product.getName())
                .calories(product.getCalories())
                .proteins(product.getProteins())
                .fats(product.getFats())
                .carbohydrates(product.getCarbohydrates())
                .category(product.getCategory())
                .cookingRequired(product.getCookingRequired())
                .flags(product.getFlags())
                .mainPhoto(mainPhoto)
                .build();
    }

    private ProductDto mapToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .calories(product.getCalories())
                .proteins(product.getProteins())
                .fats(product.getFats())
                .carbohydrates(product.getCarbohydrates())
                .composition(product.getComposition())
                .category(product.getCategory())
                .cookingRequired(product.getCookingRequired())
                .flags(product.getFlags())
                .photos(product.getPhotos().stream().map(ProductPhoto::getPhotoUrl).toList())
                .build();
    }
}
