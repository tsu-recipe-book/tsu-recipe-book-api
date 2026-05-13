package ru.nu1ts.recipebook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.nu1ts.recipebook.dto.*;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;
import ru.nu1ts.recipebook.service.DishService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
@Tag(name = "Dishes", description = "Управление блюдами")
public class DishController {

    private final DishService dishService;

    @GetMapping
    @Operation(summary = "Получить список блюд", description = "Возвращает список блюд с фильтрацией и поиском")
    public List<DishListItem> getDishes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DishCategory category,
            @RequestParam(required = false) List<DishFlag> flags,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        return dishService.getDishes(search, category, flags, sortBy, sortOrder);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить блюдо по ID")
    public DishDto getDishById(@PathVariable UUID id) {
        return dishService.getDishById(id);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Создать блюдо")
    @ResponseStatus(HttpStatus.CREATED)
    public DishDto createDish(@Valid @ModelAttribute DishCreateRequest request) {
        return dishService.createDish(request);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @Operation(summary = "Обновить блюдо")
    public DishDto updateDish(@PathVariable UUID id, @Valid @ModelAttribute DishUpdateRequest request) {
        return dishService.updateDish(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить блюдо")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDish(@PathVariable UUID id) {
        dishService.deleteDish(id);
    }

    @PostMapping("/calculate-nutrition")
    @Operation(summary = "Расчет КБЖУ блюда")
    public DishNutritionResponse calculateNutrition(@Valid @RequestBody DishNutritionCalculationRequest request) {
        return dishService.calculateNutrition(request);
    }
}
