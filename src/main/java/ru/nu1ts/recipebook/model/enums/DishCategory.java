package ru.nu1ts.recipebook.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Категория блюда")
public enum DishCategory {
    DESSERT,
    FIRST,
    SECOND,
    DRINK,
    SALAD,
    SOUP,
    SNACK
}
