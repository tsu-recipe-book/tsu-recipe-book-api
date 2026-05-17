package ru.nu1ts.recipebook.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import ru.nu1ts.recipebook.dto.DishNutritionCalculationRequest;
import ru.nu1ts.recipebook.dto.IngredientCalculationRequest;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.entity.DishIngredient;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DisplayName("API Tests: Dish Management (CRUD)")
public class DishApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Product veganProduct;
    private Product meatProduct;
    private Dish testDish;

    @BeforeEach
    void setUp() {
        veganProduct = productRepository.save(Product.builder()
                .name("Vegetable").calories(100.0).proteins(2.0).fats(0.5).carbohydrates(20.0)
                .category(ProductCategory.VEGETABLES).cookingRequired(CookingRequired.READY_TO_EAT)
                .flags(List.of(ProductFlag.VEGAN)).build());

        meatProduct = productRepository.save(Product.builder()
                .name("Meat").calories(250.0).proteins(25.0).fats(15.0).carbohydrates(0.0)
                .category(ProductCategory.MEAT).cookingRequired(CookingRequired.REQUIRES_COOKING)
                .build());

        testDish = Dish.builder()
                .name("Basic dish")
                .category(DishCategory.SALAD)
                .calories(350.0).proteins(27.0).fats(15.5).carbohydrates(20.0).portionSize(200.0)
                .build();
        testDish.addIngredient(DishIngredient.builder().product(veganProduct).weight(100.0).build());
        testDish.addIngredient(DishIngredient.builder().product(meatProduct).weight(100.0).build());
        testDish = dishRepository.save(testDish);
    }

    @DisplayName("Creation: BVA checking for dish name length")
    @ParameterizedTest(name = "Length={0}, Name=''{1}'' -> Status: {2}")
    @CsvSource({
            "1, A, 400",       // BVA: Below the border
            "2, AB, 201",      // BVA: Right on the border
            "5, Salad, 201"    // EP: Valid value
    })
    void createDish_NameLengthValidation(int length, String name, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", name)
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @DisplayName("Creation: BVA & EP checking for calories and portion size")
    @ParameterizedTest(name = "Calories={0}, Portion={1} -> Status: {2}")
    @CsvSource({
            "0.0, 100.0, 201",   // BVA: Calories at the lower limit (0)
            "-1.0, 100.0, 400",  // BVA: Calories Below Limit
            "150.0, 0.0, 400",   // BVA: Portion at invalid boundary (0)
            "150.0, 250.0, 201"  // EP: Regular valid values
    })
    void createDish_NumericValuesValidation(String calories, String portion, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Test dish")
                .param("category", "SOUP")
                .param("calories", calories)
                .param("portionSize", portion)
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("Business Logic: Dish ignores VEGAN flag if ingredients do not support it")
    void createDish_FlagInheritanceLogic() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Strange salad")
                .param("category", "SALAD")
                .param("flags", "VEGAN")
                .param("ingredients[0].productId", meatProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flags").isEmpty());
    }

    @Test
    @DisplayName("Creation: BVA checking for max photos limit (6 is invalid)")
    void createDish_MaxPhotosExceeded() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Dish with photo")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        for (int i = 0; i < 6; i++) {
            request.file(new MockMultipartFile("photos", "pic" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("Reading: Filter by category and sorting by calories")
    void getDishes_WithFiltersAndSort() throws Exception {
        Dish soup = Dish.builder()
                .name("Soup")
                .category(DishCategory.SOUP)
                .calories(150.0)
                .proteins(5.0)
                .fats(5.0)
                .carbohydrates(20.0)
                .portionSize(300.0)
                .build();
        dishRepository.save(soup);

        mockMvc.perform(MockMvcRequestBuilders.get("/dishes")
                        .param("category", "SALAD")
                        .param("sortBy", "calories")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic dish"));
    }

    @Test
    @DisplayName("Reading: Get dish by ID")
    void getDishById_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/dishes/" + testDish.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basic dish"))
                .andExpect(jsonPath("$.ingredients.length()").value(2));
    }

    @Test
    @DisplayName("Update: Successfully update dish name")
    void updateDish_Success() throws Exception {
        MockMultipartHttpServletRequestBuilder updateReq = MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/dishes/" + testDish.getId());

        updateReq.param("name", "An updated dish")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "500.0");

        mockMvc.perform(updateReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("An updated dish"));
    }

    @Test
    @DisplayName("Deletion: Successfully delete dish")
    void deleteDish_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/dishes/" + testDish.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/dishes/" + testDish.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Calculator: Correctly calculates nutrition for given ingredients (EP)")
    void calculateNutrition_Success() throws Exception {
        IngredientCalculationRequest ingredient = new IngredientCalculationRequest(veganProduct.getId(), 200.0);
        DishNutritionCalculationRequest req = new DishNutritionCalculationRequest(List.of(ingredient));

        mockMvc.perform(MockMvcRequestBuilders.post("/dishes/calculate-nutrition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portionSize").value(200.0))
                .andExpect(jsonPath("$.calories").value(200.0))
                .andExpect(jsonPath("$.proteins").value(4.0))
                .andExpect(jsonPath("$.availableFlags[0]").value("VEGAN"));
    }
}
