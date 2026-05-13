package ru.nu1ts.recipebook.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

public class BaseSpecification {

    public static void addSearchPredicate(String search, Root<?> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (search != null && !search.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }
    }

    public static void addCategoryPredicate(Object category, Root<?> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (category != null) {
            predicates.add(cb.equal(root.get("category"), category));
        }
    }

    public static <E extends Enum<E>> void addFlagsPredicate(List<E> flags, Root<?> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (flags != null && !flags.isEmpty()) {
            for (E flag : flags) {
                predicates.add(cb.isMember(flag, root.get("flags")));
            }
        }
    }
}
