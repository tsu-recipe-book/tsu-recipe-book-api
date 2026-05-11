package ru.nu1ts.recipebook.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Флаги блюда")
public enum DishFlag {
    VEGAN,
    GLUTEN_FREE,
    SUGAR_FREE
}
