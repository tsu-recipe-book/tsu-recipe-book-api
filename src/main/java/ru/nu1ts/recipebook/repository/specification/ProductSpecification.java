package ru.nu1ts.recipebook.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filter(
            String search,
            ProductCategory category,
            CookingRequired cookingRequired,
            List<ProductFlag> flags
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (cookingRequired != null) {
                predicates.add(cb.equal(root.get("cookingRequired"), cookingRequired));
            }

            if (flags != null && !flags.isEmpty()) {
                for (ProductFlag flag : flags) {
                    predicates.add(cb.isMember(flag, root.get("flags")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
