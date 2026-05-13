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

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (flags != null && !flags.isEmpty()) {
                for (DishFlag flag : flags) {
                    predicates.add(cb.isMember(flag, root.get("flags")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
