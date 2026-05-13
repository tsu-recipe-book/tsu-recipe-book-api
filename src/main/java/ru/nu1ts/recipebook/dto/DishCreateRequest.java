package ru.nu1ts.recipebook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;

@Data
@Schema(description = "Запрос на создание блюда")
public class DishCreateRequest {

    @NotBlank
    @Size(min = 2)
    @Schema(
            description = "Название блюда. Может содержать макрос категории: "
                    + "!десерт, !первое, !второе, !напиток, !салат, !суп, !перекус. "
                    + "Первый макрос задаёт категорию и удаляется из названия. "
                    + "При одновременном указании макроса и поля category — приоритет у поля category.",
            example = "!суп Борщ украинский"
    )
    private String name;

    @NotNull
    private DishCategory category;

    @Schema(
            description = "Список флагов блюда (Веган, Без глютена, Без сахара). "
                    + "Флаг может быть установлен только если все продукты в составе имеют этот флаг. "
                    + "Несовместимые флаги будут проигнорированы автоматически.",
            nullable = true
    )
    private List<DishFlag> flags;

    @NotEmpty
    @Schema(description = "Состав блюда — список продуктов с указанием количества (г) каждого в порции")
    private List<IngredientCalculationRequest> ingredients;

    @Schema(description = "Фотографии блюда (до 5 файлов: jpg, jpeg, png, webp)", nullable = true)
    private List<MultipartFile> photos;

    @Schema(
            description = "Калорийность на порцию (ккал/порцию). "
                    + "Если не передана — подставляется рассчитанное значение на основе состава.",
            nullable = true
    )
    @PositiveOrZero
    private Double calories;

    @Schema(
            description = "Белки на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение на основе состава.",
            nullable = true
    )
    @PositiveOrZero
    private Double proteins;

    @Schema(
            description = "Жиры на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение на основе состава.",
            nullable = true
    )
    @PositiveOrZero
    private Double fats;

    @Schema(
            description = "Углеводы на порцию (г/порцию). "
                    + "Если не передано — подставляется рассчитанное значение на основе состава.",
            nullable = true
    )
    @PositiveOrZero
    private Double carbohydrates;

    @Schema(
            description = "Размер порции (г). "
                    + "Если не передан — используется суммарный вес ингредиентов как черновое значение.",
            nullable = true
    )
    @Positive
    private Double portionSize;
}
