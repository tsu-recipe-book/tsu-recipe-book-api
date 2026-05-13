package ru.nu1ts.recipebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.dto.*;
import ru.nu1ts.recipebook.exception.ResourceNotFoundException;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.entity.DishIngredient;
import ru.nu1ts.recipebook.model.entity.DishPhoto;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.repository.specification.DishSpecification;
import ru.nu1ts.recipebook.util.CategoryMacroParser;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<DishListItem> getDishes(String search, DishCategory category,
                                        List<DishFlag> flags, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Specification<Dish> spec = DishSpecification.filter(search, category, flags);
        return dishRepository.findAll(spec, sort).stream()
                .map(this::mapToListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public DishDto getDishById(UUID id) {
        Dish dish = findDishOrThrow(id);
        return mapToDto(dish);
    }

    @Transactional
    public DishDto createDish(DishCreateRequest request) {
        CategoryMacroParser.ParsedName parsed =
                CategoryMacroParser.parse(request.getName(), request.getCategory());

        DishNutritionResponse nutrition =
                calculateNutrition(new DishNutritionCalculationRequest(request.getIngredients()));

        double calories     = resolveValue(request.getCalories(),      nutrition.getCalories());
        double proteins     = resolveValue(request.getProteins(),      nutrition.getProteins());
        double fats         = resolveValue(request.getFats(),          nutrition.getFats());
        double carbohydrates= resolveValue(request.getCarbohydrates(), nutrition.getCarbohydrates());
        double portionSize  = resolveValue(request.getPortionSize(),   nutrition.getPortionSize());

        Dish dish = Dish.builder()
                .name(parsed.name())
                .category(parsed.category())
                .calories(calories)
                .proteins(proteins)
                .fats(fats)
                .carbohydrates(carbohydrates)
                .portionSize(portionSize)
                .build();

        updateIngredients(dish, request.getIngredients());

        applyFlags(dish, request.getFlags(), dish.getIngredients());

        List<MultipartFile> validFiles = fileStorageService.filterValidFiles(request.getPhotos());
        if (!validFiles.isEmpty()) {
            savePhotos(dish, validFiles);
        }

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public DishDto updateDish(UUID id, DishUpdateRequest request) {
        Dish dish = findDishOrThrow(id);

        CategoryMacroParser.ParsedName parsed =
                CategoryMacroParser.parse(request.getName(), request.getCategory());

        DishNutritionResponse nutrition =
                calculateNutrition(new DishNutritionCalculationRequest(request.getIngredients()));

        double calories     = resolveValue(request.getCalories(),      nutrition.getCalories());
        double proteins     = resolveValue(request.getProteins(),      nutrition.getProteins());
        double fats         = resolveValue(request.getFats(),          nutrition.getFats());
        double carbohydrates= resolveValue(request.getCarbohydrates(), nutrition.getCarbohydrates());
        double portionSize  = resolveValue(request.getPortionSize(),   nutrition.getPortionSize());

        dish.setName(parsed.name());
        dish.setCategory(parsed.category());
        dish.setCalories(calories);
        dish.setProteins(proteins);
        dish.setFats(fats);
        dish.setCarbohydrates(carbohydrates);
        dish.setPortionSize(portionSize);

        dish.getIngredients().clear();
        updateIngredients(dish, request.getIngredients());

        applyFlags(dish, request.getFlags(), dish.getIngredients());

        List<String> keepUrls = request.getPhotosToKeep() != null
                ? Arrays.asList(request.getPhotosToKeep())
                : new ArrayList<>();
        updatePhotos(dish, keepUrls, request.getPhotos());

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public void deleteDish(UUID id) {
        Dish dish = findDishOrThrow(id);
        List<String> urls = dish.getPhotos().stream()
                .map(DishPhoto::getPhotoUrl)
                .toList();
        fileStorageService.deleteFiles(urls);
        dishRepository.delete(dish);
    }

    @Transactional(readOnly = true)
    public DishNutritionResponse calculateNutrition(DishNutritionCalculationRequest request) {
        double totalWeight = 0, totalCal = 0, totalProt = 0, totalFat = 0, totalCarb = 0;

        List<Product> products = new ArrayList<>();

        for (IngredientCalculationRequest item : request.getIngredients()) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product", item.getProductId().toString()));
            double factor = item.getWeight() / 100.0;
            totalWeight += item.getWeight();
            totalCal    += p.getCalories()      * factor;
            totalProt   += p.getProteins()       * factor;
            totalFat    += p.getFats()           * factor;
            totalCarb   += p.getCarbohydrates()  * factor;
            products.add(p);
        }

        List<DishFlag> available = calculateAvailableDishFlags(products);

        return DishNutritionResponse.builder()
                .calories(round1(totalCal))
                .proteins(round1(totalProt))
                .fats(round1(totalFat))
                .carbohydrates(round1(totalCarb))
                .portionSize(totalWeight)
                .availableFlags(available)
                .build();
    }

    private double resolveValue(Double userValue, Double calculatedValue) {
        return (userValue != null) ? userValue : calculatedValue;
    }

    private void updateIngredients(Dish dish, List<IngredientCalculationRequest> ingredients) {
        for (IngredientCalculationRequest ingReq : ingredients) {
            Product product = productRepository.findById(ingReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product", ingReq.getProductId().toString()));
            DishIngredient ing = DishIngredient.builder()
                    .product(product)
                    .weight(ingReq.getWeight())
                    .build();
            dish.addIngredient(ing);
        }
    }

    private void applyFlags(Dish dish,
                            List<DishFlag> requestedFlags,
                            List<DishIngredient> ingredients) {
        List<Product> products = ingredients.stream()
                .map(DishIngredient::getProduct)
                .toList();
        Set<DishFlag> available = new HashSet<>(calculateAvailableDishFlags(products));

        List<DishFlag> toSet = (requestedFlags != null)
                ? requestedFlags.stream().filter(available::contains).toList()
                : List.of();

        dish.setFlags(toSet);
    }

    private List<DishFlag> calculateAvailableDishFlags(List<Product> products) {
        if (products.isEmpty()) return List.of();

        Set<ProductFlag> common = new HashSet<>(products.get(0).getFlags());
        for (int i = 1; i < products.size(); i++) {
            common.retainAll(products.get(i).getFlags());
        }

        return common.stream()
                .map(pf -> {
                    try { return DishFlag.valueOf(pf.name()); }
                    catch (IllegalArgumentException ignored) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void updatePhotos(Dish dish, List<String> keepUrls, List<MultipartFile> newFiles) {
        List<MultipartFile> validNewFiles = fileStorageService.filterValidFiles(newFiles);
        int totalCount = keepUrls.size() + validNewFiles.size();
        if (totalCount > fileStorageService.getMaxFilesPerItem()) {
            throw new ru.nu1ts.recipebook.exception.BusinessException(
                    ru.nu1ts.recipebook.exception.ErrorCode.TOO_MANY_FILES,
                    "Total photos cannot exceed " + fileStorageService.getMaxFilesPerItem()
                            + ". Keeping: " + keepUrls.size()
                            + ", new: " + validNewFiles.size());
        }

        fileStorageService.deleteUnusedFiles(
                dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).toList(),
                keepUrls);
        dish.getPhotos().removeIf(p -> !keepUrls.contains(p.getPhotoUrl()));

        if (!validNewFiles.isEmpty()) {
            List<UploadedFile> uploaded = fileStorageService.saveFiles(validNewFiles);
            for (UploadedFile f : uploaded) {
                dish.addPhoto(DishPhoto.builder().photoUrl(f.getUrl()).build());
            }
        }
        for (int i = 0; i < dish.getPhotos().size(); i++) {
            dish.getPhotos().get(i).setSortOrder(i);
        }
    }

    private void savePhotos(Dish dish, List<MultipartFile> files) {
        List<UploadedFile> uploaded = fileStorageService.saveFiles(files);
        for (int i = 0; i < uploaded.size(); i++) {
            dish.addPhoto(DishPhoto.builder()
                    .photoUrl(uploaded.get(i).getUrl())
                    .sortOrder(i)
                    .build());
        }
    }

    private Dish findDishOrThrow(UUID id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dish", id.toString()));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private DishListItem mapToListItem(Dish dish) {
        return DishListItem.builder()
                .id(dish.getId())
                .name(dish.getName())
                .calories(dish.getCalories())
                .proteins(dish.getProteins())
                .fats(dish.getFats())
                .carbohydrates(dish.getCarbohydrates())
                .portionSize(dish.getPortionSize())
                .category(dish.getCategory())
                .flags(dish.getFlags())
                .mainPhoto(dish.getPhotos().isEmpty() ? null
                        : dish.getPhotos().get(0).getPhotoUrl())
                .build();
    }

    private DishDto mapToDto(Dish dish) {
        List<Product> products = dish.getIngredients().stream()
                .map(DishIngredient::getProduct)
                .toList();
        List<DishFlag> available = calculateAvailableDishFlags(products);

        return DishDto.builder()
                .id(dish.getId())
                .name(dish.getName())
                .calories(dish.getCalories())
                .proteins(dish.getProteins())
                .fats(dish.getFats())
                .carbohydrates(dish.getCarbohydrates())
                .portionSize(dish.getPortionSize())
                .category(dish.getCategory())
                .flags(dish.getFlags())
                .availableFlags(available)
                .photos(dish.getPhotos().stream()
                        .map(DishPhoto::getPhotoUrl).toList())
                .ingredients(dish.getIngredients().stream()
                        .map(ing -> new DishIngredientDto(
                                ing.getProduct().getId(),
                                ing.getProduct().getName(),
                                ing.getWeight()))
                        .toList())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .build();
    }
}
