ALTER TABLE dish_ingredients
    ADD CONSTRAINT uq_dish_ingredients_dish_product
        UNIQUE (dish_id, product_id);
