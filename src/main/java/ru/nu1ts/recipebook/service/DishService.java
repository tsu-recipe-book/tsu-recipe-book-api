package ru.nu1ts.recipebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nu1ts.recipebook.dto.DishNutritionCalculationRequest;
import ru.nu1ts.recipebook.dto.DishNutritionResponse;
import ru.nu1ts.recipebook.dto.IngredientCalculationRequest;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.ProductRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DishService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DishNutritionResponse calculateNutrition(DishNutritionCalculationRequest request) {
        double totalWeight = 0;
        double totalCalories = 0;
        double totalProteins = 0;
        double totalFats = 0;
        double totalCarbohydrates = 0;

        List<Product> products = new ArrayList<>();

        for (IngredientCalculationRequest item : request.getIngredients()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
            
            products.add(product);
            double weight = item.getWeight();
            double factor = weight / 100.0;

            totalWeight += weight;
            totalCalories += product.getCalories() * factor;
            totalProteins += product.getProteins() * factor;
            totalFats += product.getFats() * factor;
            totalCarbohydrates += product.getCarbohydrates() * factor;
        }

        List<ProductFlag> availableFlags = calculateAvailableFlags(products);

        return DishNutritionResponse.builder()
                .calories(Math.round(totalCalories * 10.0) / 10.0)
                .proteins(Math.round(totalProteins * 10.0) / 10.0)
                .fats(Math.round(totalFats * 10.0) / 10.0)
                .carbohydrates(Math.round(totalCarbohydrates * 10.0) / 10.0)
                .portionSize(totalWeight)
                .availableFlags(availableFlags)
                .build();
    }

    private List<ProductFlag> calculateAvailableFlags(List<Product> products) {
        if (products.isEmpty()) return new ArrayList<>();

        Set<ProductFlag> commonFlags = new HashSet<>(products.get(0).getFlags());

        for (int i = 1; i < products.size(); i++) {
            commonFlags.retainAll(products.get(i).getFlags());
        }

        return new ArrayList<>(commonFlags);
    }
}
