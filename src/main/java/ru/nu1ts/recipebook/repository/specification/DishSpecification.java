package ru.nu1ts.recipebook.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.ArrayList;
import java.util.List;

public class DishSpecification {
    public static Specification<Dish> filter(String search, DishCategory category, List<DishFlag> flags) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            BaseSpecification.addSearchPredicate(search, root, cb, predicates);
            BaseSpecification.addCategoryPredicate(category, root, cb, predicates);
            BaseSpecification.addFlagsPredicate(flags, root, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
