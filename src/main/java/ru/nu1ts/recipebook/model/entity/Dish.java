package ru.nu1ts.recipebook.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish implements PhotoManaged<DishPhoto> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double calories;

    @Column(nullable = false)
    private Double proteins;

    @Column(nullable = false)
    private Double fats;

    @Column(nullable = false)
    private Double carbohydrates;

    @Column(name = "portion_size", nullable = false)
    private Double portionSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DishCategory category;

    @ElementCollection(targetClass = DishFlag.class)
    @CollectionTable(name = "dish_flags", joinColumns = @JoinColumn(name = "dish_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "flag")
    @Builder.Default
    private List<DishFlag> flags = new ArrayList<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<DishPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DishIngredient> ingredients = new ArrayList<>();

    public void addPhoto(DishPhoto photo) {
        photos.add(photo);
        photo.setDish(this);
    }

    public void addIngredient(DishIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setDish(this);
    }
}
