package ru.nu1ts.recipebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nu1ts.recipebook.model.entity.DishIngredient;

import java.util.List;
import java.util.UUID;

@Repository
public interface DishIngredientRepository extends JpaRepository<DishIngredient, UUID> {

    @Query("SELECT di.dish.id, di.dish.name FROM DishIngredient di WHERE di.product.id = :productId")
    List<Object[]> findDishRefsByProductId(@Param("productId") UUID productId);
}
