package ru.nu1ts.recipebook.util;

import ru.nu1ts.recipebook.model.enums.DishCategory;

public class CategoryMacroParser {

    public record ParsedName(String name, DishCategory category) {}
    public static ParsedName parse(String originalName, DishCategory explicitCategory) {
        if (originalName == null || !originalName.startsWith("!")) {
            return new ParsedName(originalName, explicitCategory);
        }

        int firstSpace = originalName.indexOf(" ");
        if (firstSpace < 2) {
            return new ParsedName(originalName, explicitCategory);
        }

        String macro = originalName.substring(1, firstSpace).toLowerCase();
        String cleanName = originalName.substring(firstSpace + 1).trim();

        DishCategory fromMacro = switch (macro) {
            case "десерт"  -> DishCategory.DESSERT;
            case "первое"  -> DishCategory.FIRST;
            case "второе"  -> DishCategory.SECOND;
            case "напиток" -> DishCategory.DRINK;
            case "салат"   -> DishCategory.SALAD;
            case "суп"     -> DishCategory.SOUP;
            case "перекус" -> DishCategory.SNACK;
            default        -> null;
        };

        if (fromMacro == null) {
            return new ParsedName(originalName, explicitCategory);
        }

        DishCategory resolved = (explicitCategory != null) ? explicitCategory : fromMacro;
        return new ParsedName(cleanName, resolved);
    }
}
