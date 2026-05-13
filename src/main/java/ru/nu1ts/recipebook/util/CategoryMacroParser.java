package ru.nu1ts.recipebook.util;

import ru.nu1ts.recipebook.model.enums.DishCategory;

public class CategoryMacroParser {

    public record ParsedName(String name, DishCategory category) {}

    public static ParsedName parse(String originalName, DishCategory defaultCategory) {
        if (originalName == null || !originalName.startsWith("!")) {
            return new ParsedName(originalName, defaultCategory);
        }

        int firstSpace = originalName.indexOf(" ");
        if (firstSpace > 1) {
            String macro = originalName.substring(1, firstSpace).toLowerCase();
            String newName = originalName.substring(firstSpace + 1);

            DishCategory determinedCategory = switch (macro) {
                case "десерт" -> DishCategory.DESSERT;
                case "первое", "суп" -> DishCategory.SOUP;
                case "второе" -> DishCategory.SECOND;
                case "напиток" -> DishCategory.DRINK;
                case "салат" -> DishCategory.SALAD;
                case "закуска" -> DishCategory.SNACK;
                default -> defaultCategory;
            };
            return new ParsedName(newName, determinedCategory);
        }

        return new ParsedName(originalName, defaultCategory);
    }
}
