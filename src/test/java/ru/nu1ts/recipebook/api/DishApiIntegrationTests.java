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
import org.springframework.util.FileSystemUtils;
import ru.nu1ts.recipebook.dto.DishNutritionCalculationRequest;
import ru.nu1ts.recipebook.dto.IngredientCalculationRequest;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.entity.DishIngredient;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.DishFlag;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// properties = ... перенаправляет сохранение файлов в отдельную папку для тестов (изоляция файловой системы).
// webEnvironment = MOCK гарантирует, что имитация HTTP-запроса и реальная бизнес-логика выполняются в одном и том же потоке (используется по умолчанию, если ничего не указывать)
// Если использовать RANDOM_PORT, сервер запустится в отдельном потоке, и @Transactional не сможет откатить (rollback) мусор в БД после теста.
@SpringBootTest(properties = {"app.upload.upload-dir=test-uploads"}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@DisplayName("API Tests: Dish Management (CRUD)")
public class DishApiIntegrationTests {

    // Позволяет отправлять имитированные HTTP-запросы (GET, POST и т.д.) и проверять ответы (статусы, JSON-тело) без запуска реального веб-сервера
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

    @AfterAll
    static void afterAll() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("test-uploads"));
    }

    @Test
    @DisplayName("Macro: !суп in name is removed (explicit category handles @NotNull validation)")
    void createDish_MacroInName_SetsCategoryAndRemovesMacro() throws Exception {
        // Так как поле category в DTO помечено аннотацией @NotNull, мы обязаны передать его в запросе.
        // Настоящая цель этого теста — доказать, что парсер работает и удаляет макрос из названия блюда.
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "!суп Борщ")
                .param("category", "SOUP")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Борщ"))
                .andExpect(jsonPath("$.category").value("SOUP"));
    }

    @Test
    @DisplayName("Macro: explicit category overrides macro in name")
    void createDish_ExplicitCategoryOverridesMacro() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "!суп Оливье")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Оливье"))
                .andExpect(jsonPath("$.category").value("SALAD"));
    }

    @Test
    @DisplayName("Business Logic: duplicate ingredients throw validation error (422 Unprocessable Entity)")
    void createDish_DuplicateIngredients_ReturnsBadRequest() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Duplicates")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0")
                .param("ingredients[1].productId", veganProduct.getId().toString())
                .param("ingredients[1].weight", "50.0");

        mockMvc.perform(request)
                .andExpect(status().isUnprocessableContent());
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
    @ParameterizedTest(name = "Calories={0}, Portion={1}, Weight={2} -> Status: {3}")
    @CsvSource({
            "0.0, 100.0, 100.0, 201",   // BVA: Calories at the lower limit (0)
            "-1.0, 100.0, 100.0, 400",  // BVA: Calories Below Limit
            "150.0, 0.0, 100.0, 400",   // BVA: Portion at invalid boundary (0)
            "150.0, 0.001, 0.001, 201", // BVA: Portion strictly > 0. Weight proportional
            "150.0, 250.0, 100.0, 201"  // EP: Regular valid values
    })
    void createDish_NumericValuesValidation(String calories, String portion, String weight, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Test dish")
                .param("category", "SOUP")
                .param("calories", calories)
                .param("portionSize", portion)
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", weight);

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("Creation: BVA checking for exactly max photos limit (5 is valid)")
    void createDish_ExactlyMaxPhotos() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Dish with 5 photos")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        for (int i = 0; i < 5; i++) {
            request.file(new MockMultipartFile("photos", "pic" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Creation: BVA checking for max photos limit (6 is invalid)")
    void createDish_MaxPhotosExceeded() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/dishes");
        request.param("name", "Dish with 6 photos")
                .param("category", "SALAD")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0");

        for (int i = 0; i < 6; i++) {
            request.file(new MockMultipartFile("photos", "pic" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reading: Case-insensitive dish search by substring")
    void getDishes_SearchBySubstring() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/dishes")
                        .param("search", "basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic dish"));
    }

    @Test
    @DisplayName("Reading: Sort dishes by descending portion size")
    void getDishes_SortByPortionSize() throws Exception {
        Dish bigDish = Dish.builder()
                .name("Big Dish").category(DishCategory.SOUP)
                .calories(150.0).proteins(5.0).fats(5.0).carbohydrates(20.0).portionSize(500.0)
                .build();
        dishRepository.save(bigDish);

        mockMvc.perform(MockMvcRequestBuilders.get("/dishes")
                        .param("sortBy", "portionSize")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Big Dish"))
                .andExpect(jsonPath("$[1].name").value("Basic dish"));
    }

    @Test
    @DisplayName("Reading: Filter dishes by category and flags")
    void getDishes_WithFilters() throws Exception {
        Dish veganDish = Dish.builder()
                .name("Vegan soup").category(DishCategory.SOUP)
                .calories(150.0).proteins(5.0).fats(5.0).carbohydrates(20.0).portionSize(300.0)
                .flags(List.of(DishFlag.VEGAN))
                .build();
        veganDish.addIngredient(DishIngredient.builder().product(veganProduct).weight(100.0).build());
        dishRepository.save(veganDish);

        mockMvc.perform(MockMvcRequestBuilders.get("/dishes")
                        .param("category", "SOUP")
                        .param("flags", "VEGAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Vegan soup"));
    }

    @Test
    @DisplayName("Reading: Get dish by non-existent ID returns 404")
    void getDishById_NotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/dishes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update: VEGAN flag is dropped when non-vegan ingredient is added")
    void updateDish_FlagDroppedWhenIngredientAdded() throws Exception {
        Dish veganDish = Dish.builder()
                .name("Vegan salad").category(DishCategory.SALAD)
                .calories(100.0).proteins(2.0).fats(0.5).carbohydrates(20.0).portionSize(200.0)
                .flags(List.of(DishFlag.VEGAN))
                .build();
        veganDish.addIngredient(DishIngredient.builder().product(veganProduct).weight(200.0).build());
        veganDish = dishRepository.save(veganDish);

        MockMultipartHttpServletRequestBuilder updateReq = MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/dishes/" + veganDish.getId());

        updateReq.param("name", "No longer a vegan salad")
                .param("category", "SALAD")
                .param("flags", "VEGAN")
                .param("ingredients[0].productId", veganProduct.getId().toString())
                .param("ingredients[0].weight", "100.0")
                .param("ingredients[1].productId", meatProduct.getId().toString())
                .param("ingredients[1].weight", "100.0");

        mockMvc.perform(updateReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags").isEmpty());
    }

    @Test
    @DisplayName("Deletion: 404 Not Found when deleting non-existent dish")
    void deleteDish_NotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/dishes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Calculator: Correctly calculates nutrition for given ingredients")
    void calculateNutrition_Success() throws Exception {
        IngredientCalculationRequest ingredient = new IngredientCalculationRequest(veganProduct.getId(), 200.0);
        DishNutritionCalculationRequest req = new DishNutritionCalculationRequest(List.of(ingredient));

        mockMvc.perform(MockMvcRequestBuilders.post("/dishes/calculate-nutrition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portionSize").value(200.0))
                .andExpect(jsonPath("$.calories").value(200.0));
    }

    @Test
    @DisplayName("Calculator: Empty ingredients list returns 400 Bad Request")
    void calculateNutrition_EmptyIngredients_ReturnsBadRequest() throws Exception {
        DishNutritionCalculationRequest req = new DishNutritionCalculationRequest(List.of());

        mockMvc.perform(MockMvcRequestBuilders.post("/dishes/calculate-nutrition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
