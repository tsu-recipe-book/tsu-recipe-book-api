package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;

import java.util.List;

@Data
@Schema(description = "Запрос на обновление продукта")
public class ProductUpdateRequest {
    @NotBlank
    @Size(min = 2)
    @Schema(description = "Название продукта", example = "Куриное филе")
    private String name;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Калорийность на 100г", example = "110.0")
    private Double calories;

    @NotNull
    @PositiveOrZero
    @Max(100)
    @Schema(description = "Белки на 100г", example = "23.1")
    private Double proteins;

    @NotNull
    @PositiveOrZero
    @Max(100)
    @Schema(description = "Жиры на 100г", example = "1.2")
    private Double fats;

    @NotNull
    @PositiveOrZero
    @Max(100)
    @Schema(description = "Углеводы на 100г", example = "0.0")
    private Double carbohydrates;

    @Schema(description = "Состав продукта", example = "Грудка куриная, соль")
    private String composition;

    @NotNull
    private ProductCategory category;

    @NotNull
    private CookingRequired cookingRequired;

    private List<ProductFlag> flags;

    @Schema(description = "Новые фотографии продукта (загрузка)")
    private List<MultipartFile> photos;

    @Schema(
            description = "URL уже загруженных фото, которые нужно оставить",
            example = "[\"/uploads/22eba33a-28f6-4018-bb74-e40d3d249695.jpg\"]"
    )
    private List<String> photosToKeep;
}
