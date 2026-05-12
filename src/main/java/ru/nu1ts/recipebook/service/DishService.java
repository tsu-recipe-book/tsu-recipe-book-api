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
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.repository.specification.DishSpecification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return dishRepository.findAll(spec, sort).stream().map(this::mapToListItem).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DishDto getDishById(UUID id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
        return mapToDto(dish);
    }

    @Transactional
    public DishDto createDish(DishCreateRequest request) {
        DishNutritionCalculationRequest calcReq = new DishNutritionCalculationRequest(request.getIngredients());
        DishNutritionResponse nutrition = calculateNutrition(calcReq);

        Dish dish = Dish.builder()
                .name(request.getName())
                .category(request.getCategory())
                .flags(request.getFlags() != null ? request.getFlags() : new ArrayList<>())
                .calories(nutrition.getCalories())
                .proteins(nutrition.getProteins())
                .fats(nutrition.getFats())
                .carbohydrates(nutrition.getCarbohydrates())
                .portionSize(nutrition.getPortionSize())
                .build();

        for (IngredientCalculationRequest ingReq : request.getIngredients()) {
            Product product = productRepository.findById(ingReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            DishIngredient ing = DishIngredient.builder()
                    .product(product)
                    .weight(ingReq.getWeight())
                    .build();
            dish.addIngredient(ing);
        }

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            savePhotos(dish, request.getPhotos());
        }

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public DishDto updateDish(UUID id, DishUpdateRequest request) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
        
        DishNutritionCalculationRequest calcReq = new DishNutritionCalculationRequest(request.getIngredients());
        DishNutritionResponse nutrition = calculateNutrition(calcReq);

        dish.setName(request.getName());
        dish.setCategory(request.getCategory());
        dish.setFlags(request.getFlags() != null ? request.getFlags() : new ArrayList<>());
        dish.setCalories(nutrition.getCalories());
        dish.setProteins(nutrition.getProteins());
        dish.setFats(nutrition.getFats());
        dish.setCarbohydrates(nutrition.getCarbohydrates());
        dish.setPortionSize(nutrition.getPortionSize());

        dish.getIngredients().clear();
        for (IngredientCalculationRequest ingReq : request.getIngredients()) {
            Product product = productRepository.findById(ingReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            DishIngredient ing = DishIngredient.builder()
                    .product(product)
                    .weight(ingReq.getWeight())
                    .build();
            dish.addIngredient(ing);
        }

        updatePhotos(dish, request);

        return mapToDto(dishRepository.save(dish));
    }

    @Transactional
    public void deleteDish(UUID id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
        List<String> urls = dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).collect(Collectors.toList());
        fileStorageService.deleteFiles(urls);
        dishRepository.delete(dish);
    }

    private void updatePhotos(Dish dish, DishUpdateRequest request) {
        List<String> keepUrls = request.getPhotosToKeep() != null ? Arrays.asList(request.getPhotosToKeep()) : new ArrayList<>();
        List<String> urlsToDelete = dish.getPhotos().stream()
                .map(DishPhoto::getPhotoUrl)
                .filter(url -> !keepUrls.contains(url))
                .collect(Collectors.toList());
        
        if (!urlsToDelete.isEmpty()) {
            fileStorageService.deleteFiles(urlsToDelete);
        }
        dish.getPhotos().removeIf(p -> !keepUrls.contains(p.getPhotoUrl()));

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<UploadedFile> uploaded = fileStorageService.saveFiles(request.getPhotos());
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
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getProductId()));
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
                .photos(dish.getPhotos().stream().map(DishPhoto::getPhotoUrl).collect(Collectors.toList()))
                .ingredients(dish.getIngredients().stream().map(ing -> new DishIngredientDto(
                        ing.getProduct().getId(), ing.getProduct().getName(), ing.getWeight()
                )).collect(Collectors.toList()))
                .build();
    }
}
