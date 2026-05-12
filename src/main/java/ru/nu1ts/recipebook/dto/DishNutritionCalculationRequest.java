package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на расчет КБЖУ блюда")
public class DishNutritionCalculationRequest {
    @NotEmpty
    @Schema(description = "Список ингредиентов")
    private List<IngredientCalculationRequest> ingredients;
}
