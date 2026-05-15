package ru.nu1ts.recipebook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nu1ts.recipebook.dto.DishNutritionCalculationRequest;
import ru.nu1ts.recipebook.dto.DishNutritionResponse;
import ru.nu1ts.recipebook.dto.IngredientCalculationRequest;
import ru.nu1ts.recipebook.exception.ResourceNotFoundException;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.DishFlag;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;
import ru.nu1ts.recipebook.service.DishService;
import ru.nu1ts.recipebook.service.FileStorageService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishServiceTests {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private DishRepository dishRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DishService dishService;

    private UUID validProductId;
    private Product defaultProduct;

    @BeforeEach
    void setUp() {
        validProductId = UUID.randomUUID();
        defaultProduct = Product.builder()
                .id(validProductId)
                .name("Test Product")
                .calories(250.0)
                .proteins(10.0)
                .fats(5.0)
                .carbohydrates(40.0)
                .flags(List.of(ProductFlag.VEGAN, ProductFlag.GLUTEN_FREE))
                .build();
    }

    // Techniques applied: Equivalence Partitioning (EP) and Boundary Value Analysis (BVA)
    @ParameterizedTest(name = "Weight: {0}g -> Expected: Cal:{1}, Prot:{2}, Fat:{3}, Carb:{4}")
    @CsvSource({
            "100.0,   250.0,  10.0,    5.0,   40.0",   // EP: Standard portion (multiplier 1.0)
            "50.0,    125.0,  5.0,     2.5,   20.0",   // EP: Fractional weight (multiplier 0.5)
            "0.1,     0.3,    0.0,     0.0,   0.0",    // BVA: Minimum weight (rounding check)
            "10000.0, 25000.0,1000.0,  500.0, 4000.0"  // BVA: Huge weight
    })
    @DisplayName("Calculate nutrition for a single ingredient")
    void calculateNutrition_SingleIngredient_CalculatesCorrectly(
            double weight, double expCal, double expProt, double expFat, double expCarb) {

        when(productRepository.findById(validProductId)).thenReturn(Optional.of(defaultProduct));

        IngredientCalculationRequest ingredient = new IngredientCalculationRequest(validProductId, weight);
        DishNutritionCalculationRequest request = new DishNutritionCalculationRequest(List.of(ingredient));

        DishNutritionResponse response = dishService.calculateNutrition(request);

        assertAll(
                () -> assertEquals(weight, response.getPortionSize(), "Portion weight calculated incorrectly"),
                () -> assertEquals(expCal, response.getCalories(), "Calories calculated incorrectly"),
                () -> assertEquals(expProt, response.getProteins(), "Proteins calculated incorrectly"),
                () -> assertEquals(expFat, response.getFats(), "Fats calculated incorrectly"),
                () -> assertEquals(expCarb, response.getCarbohydrates(), "Carbohydrates calculated incorrectly")
        );
    }

    @Test
    @DisplayName("Sum nutrition for multiple ingredients")
    void calculateNutrition_MultipleIngredients_SumsCorrectly() {
        UUID product2Id = UUID.randomUUID();
        Product product2 = Product.builder()
                .id(product2Id).calories(100.0).proteins(20.0).fats(2.0).carbohydrates(0.0)
                .flags(List.of(ProductFlag.VEGAN))
                .build();

        when(productRepository.findById(validProductId)).thenReturn(Optional.of(defaultProduct));
        when(productRepository.findById(product2Id)).thenReturn(Optional.of(product2));

        List<IngredientCalculationRequest> ingredients = List.of(
                new IngredientCalculationRequest(validProductId, 200.0),
                new IngredientCalculationRequest(product2Id, 50.0)
        );

        DishNutritionResponse response = dishService.calculateNutrition(new DishNutritionCalculationRequest(ingredients));

        assertAll(
                () -> assertEquals(250.0, response.getPortionSize()),
                () -> assertEquals(550.0, response.getCalories()),
                () -> assertEquals(30.0, response.getProteins()),
                () -> assertEquals(11.0, response.getFats()),
                () -> assertEquals(80.0, response.getCarbohydrates())
        );
    }

    @Test
    @DisplayName("Dish receives only common flags of all ingredients (intersection)")
    void calculateNutrition_IntersectionOfFlags() {
        UUID p2Id = UUID.randomUUID();
        Product p2 = Product.builder()
                .id(p2Id).calories(0.0).proteins(0.0).fats(0.0).carbohydrates(0.0)
                .flags(List.of(ProductFlag.VEGAN)) // Only VEGAN, no GLUTEN_FREE
                .build();

        when(productRepository.findById(validProductId)).thenReturn(Optional.of(defaultProduct));
        when(productRepository.findById(p2Id)).thenReturn(Optional.of(p2));

        DishNutritionCalculationRequest request = new DishNutritionCalculationRequest(List.of(
                new IngredientCalculationRequest(validProductId, 100.0),
                new IngredientCalculationRequest(p2Id, 100.0)
        ));

        DishNutritionResponse response = dishService.calculateNutrition(request);

        assertTrue(response.getAvailableFlags().contains(DishFlag.VEGAN));
        assertFalse(response.getAvailableFlags().contains(DishFlag.GLUTEN_FREE));
        assertEquals(1, response.getAvailableFlags().size());
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException if product not found")
    void calculateNutrition_ProductNotFound_ThrowsException() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());
        DishNutritionCalculationRequest request = new DishNutritionCalculationRequest(List.of(
                new IngredientCalculationRequest(UUID.randomUUID(), 100.0)
        ));

        assertThrows(ResourceNotFoundException.class, () -> dishService.calculateNutrition(request));
    }

    @Test
    @DisplayName("Return zeros for empty ingredient list")
    void calculateNutrition_EmptyIngredientsList_ReturnsZeros() {
        DishNutritionResponse response = dishService.calculateNutrition(
                new DishNutritionCalculationRequest(List.of())
        );

        assertAll(
                () -> assertEquals(0.0, response.getPortionSize()),
                () -> assertEquals(0.0, response.getCalories()),
                () -> assertEquals(0.0, response.getProteins()),
                () -> assertTrue(response.getAvailableFlags().isEmpty())
        );
    }
}
