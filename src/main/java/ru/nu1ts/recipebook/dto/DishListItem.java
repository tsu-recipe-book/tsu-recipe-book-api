package ru.nu1ts.recipebook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishListItem {
    private UUID id;
    private String name;
    private Double calories;
    private Double proteins;
    private Double fats;
    private Double carbohydrates;
    private Double portionSize;
    private DishCategory category;
    private List<DishFlag> flags;
    private String mainPhoto;
}
