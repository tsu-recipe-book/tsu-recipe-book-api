CREATE TABLE products (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    calories        DOUBLE PRECISION NOT NULL,
    proteins        DOUBLE PRECISION NOT NULL,
    fats            DOUBLE PRECISION NOT NULL,
    carbohydrates   DOUBLE PRECISION NOT NULL,
    composition     TEXT,
    category        VARCHAR(50) NOT NULL,
    cooking_required VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE product_flags (
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    flag        VARCHAR(50) NOT NULL
);

CREATE INDEX idx_product_flags_product_id ON product_flags(product_id);

CREATE TABLE product_photos (
    id          BIGSERIAL PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    photo_url   VARCHAR(512) NOT NULL,
    sort_order  INT NOT NULL
);

CREATE INDEX idx_product_photos_product_id ON product_photos(product_id);