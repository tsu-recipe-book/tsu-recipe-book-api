package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;

@Data
public class DishUpdateRequest {

    @NotBlank
    @Size(min = 2)
    private String name;

    @NotNull
    private DishCategory category;

    @Schema(
            description = "Список флагов блюда. "
                    + "Если передан — применяются только допустимые по составу флаги из этого списка. "
                    + "Если не передан (null) — существующие флаги пересчитываются автоматически: "
                    + "несовместимые с новым составом снимаются, совместимые остаются.",
            nullable = true
    )
    private List<DishFlag> flags;

    @NotEmpty
    private List<IngredientCalculationRequest> ingredients;

    @Schema(description = "Новые фотографии блюда")
    private List<MultipartFile> photos;

    @Schema(
            description = "URL уже загруженных фото, которые нужно оставить",
            example = "[\"/uploads/22eba33a-28f6-4018-bb74-e40d3d249695.jpg\"]"
    )
    private String[] photosToKeep;

    @Schema(
            description = "Калорийность на порцию (ккал/порцию). "
                    + "Если не передана — подставляется рассчитанное значение.",
            nullable = true
    )
    @PositiveOrZero
    private Double calories;

    @Schema(
            description = "Белки на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение.",
            nullable = true
    )
    @PositiveOrZero
    private Double proteins;

    @Schema(
            description = "Жиры на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение.",
            nullable = true
    )
    @PositiveOrZero
    private Double fats;

    @Schema(
            description = "Углеводы на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение.",
            nullable = true
    )
    @PositiveOrZero
    private Double carbohydrates;

    @Schema(
            description = "Размер порции (г). "
                    + "Если не передан — подставляется суммарный вес ингредиентов как черновое значение.",
            nullable = true
    )
    @Positive
    private Double portionSize;
}
