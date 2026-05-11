package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;

import java.util.List;

@Data
@Schema(description = "Запрос на создание продукта")
public class ProductCreateRequest {
    @NotBlank
    @Schema(description = "Название продукта", example = "Куриное филе")
    private String name;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Калорийность на 100г", example = "110.0")
    private Double calories;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Белки на 100г", example = "23.1")
    private Double proteins;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Жиры на 100г", example = "1.2")
    private Double fats;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Углеводы на 100г", example = "0.0")
    private Double carbohydrates;

    @Schema(description = "Состав продукта", example = "Грудка куриная, соль")
    private String composition;

    @NotNull
    private ProductCategory category;

    @NotNull
    private CookingRequired cookingRequired;

    private List<ProductFlag> flags;

    private List<MultipartFile> photos;
}
