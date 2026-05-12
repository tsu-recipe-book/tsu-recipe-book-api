package ru.nu1ts.recipebook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nu1ts.recipebook.dto.DishNutritionCalculationRequest;
import ru.nu1ts.recipebook.dto.DishNutritionResponse;
import ru.nu1ts.recipebook.service.DishService;

@RestController
@RequestMapping("/api/v1/dishes")
@RequiredArgsConstructor
@Tag(name = "Dishes", description = "Управление блюдами")
public class DishController {

    private final DishService dishService;

    @PostMapping("/calculate-nutrition")
    @Operation(summary = "Расчет КБЖУ блюда", description = "Рассчитывает суммарные показатели КБЖУ и доступные флаги на основе ингредиентов")
    public DishNutritionResponse calculateNutrition(@Valid @RequestBody DishNutritionCalculationRequest request) {
        return dishService.calculateNutrition(request);
    }
}
