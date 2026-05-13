package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат расчета КБЖУ блюда")
public class DishNutritionResponse {

    @Schema(description = "Калории на порцию", example = "350.5")
    private Double calories;

    @Schema(description = "Белки на порцию", example = "25.0")
    private Double proteins;

    @Schema(description = "Жиры на порцию", example = "10.5")
    private Double fats;

    @Schema(description = "Углеводы на порцию", example = "40.2")
    private Double carbohydrates;

    @Schema(description = "Общий вес порции (сумма весов ингредиентов)", example = "450.0")
    private Double portionSize;

    @Schema(description = "Флаги, доступные для этого блюда (есть у ВСЕХ продуктов)")
    private List<DishFlag> availableFlags;
}
