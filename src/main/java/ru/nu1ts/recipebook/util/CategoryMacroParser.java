package ru.nu1ts.recipebook.util;

import ru.nu1ts.recipebook.model.enums.DishCategory;

public class CategoryMacroParser {

    public record ParsedName(String name, DishCategory category) {}
    public static ParsedName parse(String originalName, DishCategory explicitCategory) {
        if (originalName == null) {
            return new ParsedName(null, explicitCategory);
        }

        String[] parts = originalName.split("\\s+");
        DishCategory fromMacro = null;
        StringBuilder cleanNameBuilder = new StringBuilder();

        for (String part : parts) {
            if (part.startsWith("!")) {
                DishCategory category = getDishCategory(part);
                if (category != null && fromMacro == null) {
                    fromMacro = category;
                } else if (category == null) {
                    if (!cleanNameBuilder.isEmpty()) cleanNameBuilder.append(" ");
                    cleanNameBuilder.append(part);
                }
            } else {
                if (!cleanNameBuilder.isEmpty()) cleanNameBuilder.append(" ");
                cleanNameBuilder.append(part);
            }
        }

        String cleanName = cleanNameBuilder.toString().trim();

        if (fromMacro == null) {
            return new ParsedName(originalName, explicitCategory);
        }

        DishCategory resolved = (explicitCategory != null) ? explicitCategory : fromMacro;
        return new ParsedName(cleanName, resolved);
    }

    private static DishCategory getDishCategory(String part) {
        String macro = part.substring(1).toLowerCase();
        return switch (macro) {
            case "десерт" -> DishCategory.DESSERT;
            case "первое" -> DishCategory.FIRST;
            case "второе" -> DishCategory.SECOND;
            case "напиток" -> DishCategory.DRINK;
            case "салат" -> DishCategory.SALAD;
            case "суп" -> DishCategory.SOUP;
            case "перекус" -> DishCategory.SNACK;
            default -> null;
        };
    }
}
