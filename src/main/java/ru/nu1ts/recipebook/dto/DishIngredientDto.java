package ru.nu1ts.recipebook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishIngredientDto {
    private UUID productId;
    private String productName;
    private Double weight;
}
