package ru.nu1ts.recipebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.dto.ProductCreateRequest;
import ru.nu1ts.recipebook.dto.ProductDto;
import ru.nu1ts.recipebook.dto.ProductListItem;
import ru.nu1ts.recipebook.dto.ProductUpdateRequest;
import ru.nu1ts.recipebook.dto.UploadedFile;
import ru.nu1ts.recipebook.exception.ResourceNotFoundException;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.entity.ProductPhoto;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.repository.specification.ProductSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
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
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return mapToDto(product);
    }

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
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

        List<MultipartFile> validFiles = filterValidFiles(request.getPhotos());
        if (!validFiles.isEmpty()) {
            savePhotos(product, validFiles);
        }

        return mapToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(UUID productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setName(request.getName());
        product.setCalories(request.getCalories());
        product.setProteins(request.getProteins());
        product.setFats(request.getFats());
        product.setCarbohydrates(request.getCarbohydrates());
        product.setComposition(request.getComposition());
        product.setCategory(request.getCategory());
        product.setCookingRequired(request.getCookingRequired());
        product.setFlags(request.getFlags() != null ? request.getFlags() : new ArrayList<>());

        updatePhotos(product, request);

        return mapToDto(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        List<String> urls = product.getPhotos().stream()
                .map(ProductPhoto::getPhotoUrl)
                .collect(Collectors.toList());
        fileStorageService.deleteFiles(urls);

        productRepository.delete(product);
    }

    private void updatePhotos(Product product, ProductUpdateRequest request) {
        List<String> keepUrls = request.getPhotosToKeep() != null
                ? request.getPhotosToKeep()
                : new ArrayList<>();

        List<String> urlsToDelete = product.getPhotos().stream()
                .map(ProductPhoto::getPhotoUrl)
                .filter(url -> !keepUrls.contains(url))
                .collect(Collectors.toList());

        if (!urlsToDelete.isEmpty()) {
            fileStorageService.deleteFiles(urlsToDelete);
        }

        product.getPhotos().removeIf(photo -> !keepUrls.contains(photo.getPhotoUrl()));

        List<MultipartFile> validNewFiles = filterValidFiles(request.getPhotos());
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

    private List<MultipartFile> filterValidFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
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
                .photos(product.getPhotos().stream().map(ProductPhoto::getPhotoUrl).collect(Collectors.toList()))
                .build();
    }
}
