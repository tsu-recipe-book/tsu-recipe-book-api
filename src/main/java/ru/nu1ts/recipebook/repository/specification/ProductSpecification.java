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

            BaseSpecification.addSearchPredicate(search, root, cb, predicates);
            BaseSpecification.addCategoryPredicate(category, root, cb, predicates);

            if (cookingRequired != null) {
                predicates.add(cb.equal(root.get("cookingRequired"), cookingRequired));
            }

            BaseSpecification.addFlagsPredicate(flags, root, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
