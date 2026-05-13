package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Краткая информация о продукте для списка")
public class ProductListItem {
    @Schema(description = "UUID продукта", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Название продукта", example = "Куриное филе")
    private String name;
    
    @Schema(description = "Калорийность на 100г", example = "110.0")
    private Double calories;
    
    @Schema(description = "Белки на 100г", example = "23.1")
    private Double proteins;
    
    @Schema(description = "Жиры на 100г", example = "1.2")
    private Double fats;
    
    @Schema(description = "Углеводы на 100г", example = "0.0")
    private Double carbohydrates;
    
    @Schema(description = "Категория продукта")
    private ProductCategory category;
    
    @Schema(description = "Степень готовности")
    private CookingRequired cookingRequired;
    
    @Schema(description = "Дополнительные флаги")
    private List<ProductFlag> flags;
    
    @Schema(description = "URL основного фото", example = "/uploads/photo.jpg")
    private String mainPhoto;
}
