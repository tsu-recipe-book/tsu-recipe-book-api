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
import ru.nu1ts.recipebook.exception.BusinessException;
import ru.nu1ts.recipebook.exception.ErrorCode;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.repository.specification.DishSpecification;
import ru.nu1ts.recipebook.util.CategoryMacroParser;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<DishListItem> getDishes(String search, DishCategory category, List<DishFlag> flags, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Specification<Dish> spec = DishSpecification.filter(search, category, flags);
        return dishRepository.findAll(spec, sort).stream().map(this::mapToListItem).toList();
    }

    @Transactional(readOnly = true)
    public DishDto getDishById(UUID id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish", id.toString()));
        return mapToDto(dish);
    }

    @Transactional
    public DishDto createDish(DishCreateRequest request) {
        CategoryMacroParser.ParsedName parsed = CategoryMacroParser.parse(request.getName(), request.getCategory());

        DishNutritionCalculationRequest calcReq = new DishNutritionCalculationRequest(request.getIngredients());
        DishNutritionResponse nutrition = calculateNutrition(calcReq);

        validateNutrition(nutrition);

        Dish dish = Dish.builder()
                .name(parsed.name())
                .category(parsed.category())
                .calories(nutrition.getCalories())
                .proteins(nutrition.getProteins())
                .fats(nutrition.getFats())
                .carbohydrates(nutrition.getCarbohydrates())
                .portionSize(nutrition.getPortionSize())
                .build();

        updateIngredients(dish, request.getIngredients());
        updateFlagsFromIngredients(dish);

        List<MultipartFile> validFiles = fileStorageService.filterValidFiles(request.getPhotos());
        if (!validFiles.isEmpty()) {
            savePhotos(dish, validFiles);
        }

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public DishDto updateDish(UUID id, DishUpdateRequest request) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish", id.toString()));

        CategoryMacroParser.ParsedName parsed = CategoryMacroParser.parse(request.getName(), request.getCategory());

        DishNutritionCalculationRequest calcReq = new DishNutritionCalculationRequest(request.getIngredients());
        DishNutritionResponse nutrition = calculateNutrition(calcReq);

        validateNutrition(nutrition);

        dish.setName(parsed.name());
        dish.setCategory(parsed.category());
        dish.setCalories(nutrition.getCalories());
        dish.setProteins(nutrition.getProteins());
        dish.setFats(nutrition.getFats());
        dish.setCarbohydrates(nutrition.getCarbohydrates());
        dish.setPortionSize(nutrition.getPortionSize());

        dish.getIngredients().clear();
        updateIngredients(dish, request.getIngredients());
        updateFlagsFromIngredients(dish);

        List<String> keepUrls = request.getPhotosToKeep() != null ? Arrays.asList(request.getPhotosToKeep()) : new ArrayList<>();
        updatePhotos(dish, keepUrls, request.getPhotos());

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public void deleteDish(UUID id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish", id.toString()));
        List<String> urls = dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).toList();
        fileStorageService.deleteFiles(urls);
        dishRepository.delete(dish);
    }

    private void updateIngredients(Dish dish, List<IngredientCalculationRequest> ingredients) {
        for (IngredientCalculationRequest ingReq : ingredients) {
            Product product = productRepository.findById(ingReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", ingReq.getProductId().toString()));
            DishIngredient ing = DishIngredient.builder()
                    .product(product)
                    .weight(ingReq.getWeight())
                    .build();
            dish.addIngredient(ing);
        }
    }

    private void updateFlagsFromIngredients(Dish dish) {
        List<ProductFlag> commonProductFlags = calculateCommonProductFlags(dish.getIngredients());
        List<DishFlag> validFlags = new ArrayList<>();
        for (ProductFlag pf : commonProductFlags) {
            try {
                validFlags.add(DishFlag.valueOf(pf.name()));
            } catch (IllegalArgumentException ignored) {}
        }
        dish.setFlags(validFlags);
    }

    private void updatePhotos(Dish dish, List<String> keepUrls, List<MultipartFile> newFiles) {
        fileStorageService.deleteUnusedFiles(dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).toList(), keepUrls);
        dish.getPhotos().removeIf(p -> !keepUrls.contains(p.getPhotoUrl()));

        List<MultipartFile> validNewFiles = fileStorageService.filterValidFiles(newFiles);
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
            dish.addPhoto(DishPhoto.builder().photoUrl(uploaded.get(i).getUrl()).sortOrder(i).build());
        }
    }

    @Transactional(readOnly = true)
    public DishNutritionResponse calculateNutrition(DishNutritionCalculationRequest request) {
        double totalWeight = 0, totalCal = 0, totalProt = 0, totalFat = 0, totalCarb = 0;

        for (IngredientCalculationRequest item : request.getIngredients()) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId().toString()));
            double f = item.getWeight() / 100.0;
            totalWeight += item.getWeight();
            totalCal += p.getCalories() * f;
            totalProt += p.getProteins() * f;
            totalFat += p.getFats() * f;
            totalCarb += p.getCarbohydrates() * f;
        }

        return DishNutritionResponse.builder()
                .calories(Math.round(totalCal * 10.0) / 10.0)
                .proteins(Math.round(totalProt * 10.0) / 10.0)
                .fats(Math.round(totalFat * 10.0) / 10.0)
                .carbohydrates(Math.round(totalCarb * 10.0) / 10.0)
                .portionSize(totalWeight)
                .build();
    }

    private void validateNutrition(DishNutritionResponse nutrition) {
        if ((nutrition.getProteins() + nutrition.getFats() + nutrition.getCarbohydrates()) * 100.0 / nutrition.getPortionSize() > 100.0) {
            throw new BusinessException(ErrorCode.BJU_SUM_EXCEEDED, "The amount of BJU per 100 grams of the finished dish cannot exceed 100");
        }
    }

    private List<ProductFlag> calculateCommonProductFlags(List<DishIngredient> ingredients) {
        if (ingredients.isEmpty()) return new ArrayList<>();

        List<Product> products = ingredients.stream()
                .map(DishIngredient::getProduct)
                .toList();

        Set<ProductFlag> commonFlags = new HashSet<>(products.get(0).getFlags());
        for (int i = 1; i < products.size(); i++) {
            commonFlags.retainAll(products.get(i).getFlags());
        }
        return new ArrayList<>(commonFlags);
    }

    private DishListItem mapToListItem(Dish dish) {
        return DishListItem.builder()
                .id(dish.getId()).name(dish.getName())
                .calories(dish.getCalories()).proteins(dish.getProteins())
                .fats(dish.getFats()).carbohydrates(dish.getCarbohydrates())
                .category(dish.getCategory()).flags(dish.getFlags())
                .mainPhoto(dish.getPhotos().isEmpty() ? null : dish.getPhotos().get(0).getPhotoUrl())
                .build();
    }

    private DishDto mapToDto(Dish dish) {
        return DishDto.builder()
                .id(dish.getId()).name(dish.getName())
                .calories(dish.getCalories()).proteins(dish.getProteins())
                .fats(dish.getFats()).carbohydrates(dish.getCarbohydrates())
                .portionSize(dish.getPortionSize()).category(dish.getCategory())
                .flags(dish.getFlags())
                .photos(dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).toList())
                .ingredients(dish.getIngredients().stream().map(ing -> new DishIngredientDto(
                        ing.getProduct().getId(), ing.getProduct().getName(), ing.getWeight()
                )).toList())
                .build();
    }
}
