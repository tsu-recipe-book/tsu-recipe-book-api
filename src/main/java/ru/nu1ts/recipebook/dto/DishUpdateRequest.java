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
}
