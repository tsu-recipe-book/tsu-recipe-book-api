package ru.nu1ts.recipebook.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;

@Data
public class DishCreateRequest {
    @NotBlank
    @Size(min = 2)
    private String name;

    @NotNull
    private DishCategory category;

    private List<DishFlag> flags;

    @NotEmpty
    private List<IngredientCalculationRequest> ingredients;

    private List<MultipartFile> photos;
}
