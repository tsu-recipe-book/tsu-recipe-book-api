package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ингредиент для расчета КБЖУ")
public class IngredientCalculationRequest {
    @NotNull
    @Schema(description = "ID продукта", example = "562fb9f7-f94c-4a98-9ded-d320a57c1665")
    private UUID productId;

    @NotNull
    @Positive
    @Schema(description = "Вес ингредиента в граммах", example = "200.0")
    private Double weight;
}
