package ru.nu1ts.recipebook.exception;

import lombok.Getter;
import ru.nu1ts.recipebook.dto.DishReference;

import java.util.List;

@Getter
public class ProductDeleteConflictException extends BusinessException {

    private final List<DishReference> usedInDishes;

    public ProductDeleteConflictException(List<DishReference> usedInDishes) {
        super(ErrorCode.PRODUCT_IN_USE,
                "Cannot delete product: it is used in one or more dishes");
        this.usedInDishes = usedInDishes;
    }
}