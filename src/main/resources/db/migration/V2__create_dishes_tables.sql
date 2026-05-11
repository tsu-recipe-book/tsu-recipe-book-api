CREATE TABLE dishes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    calories DOUBLE PRECISION NOT NULL,
    proteins DOUBLE PRECISION NOT NULL,
    fats DOUBLE PRECISION NOT NULL,
    carbohydrates DOUBLE PRECISION NOT NULL,
    portion_size DOUBLE PRECISION NOT NULL,
    category VARCHAR(50) NOT NULL
);

CREATE TABLE dish_flags (
    dish_id UUID NOT NULL,
    flag VARCHAR(50) NOT NULL,
    CONSTRAINT fk_dish_flags_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
);

CREATE TABLE dish_photos (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL,
    photo_url VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL,
    CONSTRAINT fk_dish_photos_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE
);

CREATE TABLE dish_ingredients (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL,
    product_id UUID NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_dish_ingredients_dish FOREIGN KEY (dish_id) REFERENCES dishes (id) ON DELETE CASCADE,
    CONSTRAINT fk_dish_ingredients_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);
