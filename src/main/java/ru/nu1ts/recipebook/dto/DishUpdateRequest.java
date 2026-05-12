package ru.nu1ts.recipebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;

@Data
public class DishUpdateRequest {
    @NotBlank
    private String name;

    @NotNull
    private DishCategory category;

    private List<DishFlag> flags;

    @NotEmpty
    private List<IngredientCalculationRequest> ingredients;

    private List<MultipartFile> photos;

    private String[] photosToKeep;
}
