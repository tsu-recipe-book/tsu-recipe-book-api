package ru.nu1ts.recipebook.api;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.entity.DishIngredient;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// properties = ... перенаправляет сохранение файлов в отдельную папку для тестов (изоляция файловой системы).
// webEnvironment = MOCK гарантирует, что имитация HTTP-запроса и реальная бизнес-логика выполняются в одном и том же потоке (используется по умолчанию, если ничего не указывать)
// Если использовать RANDOM_PORT, сервер запустится в отдельном потоке, и @Transactional не сможет откатить (rollback) мусор в БД после теста.
@SpringBootTest(properties = {"app.upload.upload-dir=test-uploads"}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@DisplayName("API Tests: Product Management (CRUD)")
public class ProductApiIntegrationTests {

    // Позволяет отправлять имитированные HTTP-запросы (GET, POST и т.д.) и проверять ответы (статусы, JSON-тело) без запуска реального веб-сервера
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishRepository dishRepository;

    private Product testProduct;

    @AfterAll
    static void afterAll() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("test-uploads"));
    }

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("Basic product")
                .calories(100.0)
                .proteins(10.0)
                .fats(5.0)
                .carbohydrates(15.0)
                .category(ProductCategory.VEGETABLES)
                .cookingRequired(CookingRequired.READY_TO_EAT)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @DisplayName("Creation: BVA & EP checking for BJU sum and negative values")
    @ParameterizedTest(name = "B={0}, F={1}, U={2} -> Expected status: {3}")
    @CsvSource({
            "0.0, 0.0, 0.0, 201",       // BVA: Min. border
            "50.0, 25.0, 20.0, 201",    // EP:  Valid class
            "33.3, 33.3, 33.4, 201",    // BVA: Upper limit (100.0)
            "33.4, 33.4, 33.3, 422",    // BVA: Exceed limit (100.1)
            "150.0, 0.0, 0.0, 400",     // EP:  Invalid class (single value > 100)
            "-0.1, 10.0, 10.0, 400"     // BVA: Negative value
    })
    void createProduct_BjuSumValidation(String proteins, String fats, String carbs, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");
        request.param("name", "Test product BJU")
                .param("calories", "150.0")
                .param("proteins", proteins)
                .param("fats", fats)
                .param("carbohydrates", carbs)
                .param("category", "MEAT")
                .param("cookingRequired", "READY_TO_EAT");

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @DisplayName("Creation: BVA checking for product name length")
    @ParameterizedTest(name = "Name length={0}, value=''{1}'' -> Expected status: {2}")
    @CsvSource({
            "1, A, 400",     // BVA: Below the border
            "2, AB, 201",    // BVA: Right on the border
            "6, Tomato, 201" // EP:  Valid value
    })
    void createProduct_NameLengthValidation(int length, String name, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");
        request.param("name", name)
                .param("calories", "100.0")
                .param("proteins", "10.0")
                .param("fats", "5.0")
                .param("carbohydrates", "15.0")
                .param("category", "VEGETABLES")
                .param("cookingRequired", "READY_TO_EAT");

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("Creation: BVA checking for exactly max photos (5 is valid)")
    void createProduct_ExactlyMaxPhotos() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");
        request.param("name", "Product with photo")
                .param("calories", "10.0").param("proteins", "1.0")
                .param("fats", "1.0").param("carbohydrates", "1.0")
                .param("category", "VEGETABLES")
                .param("cookingRequired", "READY_TO_EAT");

        for (int i = 0; i < 5; i++) {
            request.file(new MockMultipartFile("photos", "pic" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Creation: BVA checking for max photos limit exceeded (6 is invalid)")
    void createProduct_MaxPhotosLimitExceeded() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");
        request.param("name", "A product with a bunch of photos")
                .param("calories", "10.0").param("proteins", "1.0")
                .param("fats", "1.0").param("carbohydrates", "1.0")
                .param("category", "VEGETABLES")
                .param("cookingRequired", "READY_TO_EAT");

        for (int i = 0; i < 6; i++) {
            request.file(new MockMultipartFile("photos", "pic" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reading: Get product by ID")
    void getProductById_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basic product"));
    }

    @Test
    @DisplayName("Reading: Case-insensitive search by substring")
    void getProductsList_CaseInsensitiveSearch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                        .param("search", "basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic product"));
    }

    @Test
    @DisplayName("Reading: Filter products by category, cookingRequired and flags")
    void getProductsList_WithMultipleFilters() throws Exception {
        testProduct.getFlags().add(ProductFlag.VEGAN);
        productRepository.save(testProduct);

        productRepository.save(Product.builder()
                .name("Meat").calories(200.0).proteins(20.0).fats(10.0).carbohydrates(0.0)
                .category(ProductCategory.MEAT).cookingRequired(CookingRequired.REQUIRES_COOKING)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                        .param("category", "VEGETABLES")
                        .param("flags", "VEGAN")
                        .param("cookingRequired", "READY_TO_EAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic product"));
    }

    @Test
    @DisplayName("Update: Successfully update product name and category")
    void updateProduct_Success() throws Exception {
        MockMultipartHttpServletRequestBuilder updateReq = MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/products/" + testProduct.getId());

        updateReq.param("name", "Updated Name")
                .param("calories", "150.0")
                .param("proteins", "10.0")
                .param("fats", "5.0")
                .param("carbohydrates", "15.0")
                .param("category", "MEAT")
                .param("cookingRequired", "READY_TO_EAT");

        mockMvc.perform(updateReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.category").value("MEAT"));
    }

    @Test
    @DisplayName("Deletion: Successfully delete product")
    void deleteProduct_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deletion: 404 Not Found when deleting non-existent product")
    void deleteProduct_NotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deletion: 409 Conflict when product is used in a dish")
    void deleteProduct_ConflictWhenUsedInDish() throws Exception {
        Dish testDish = Dish.builder()
                .name("Салат")
                .calories(100.0).proteins(10.0).fats(5.0).carbohydrates(15.0).portionSize(200.0)
                .category(DishCategory.SALAD).build();
        testDish.addIngredient(DishIngredient.builder().product(testProduct).weight(100.0).build());
        dishRepository.save(testDish);

        mockMvc.perform(MockMvcRequestBuilders.delete("/products/" + testProduct.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }
}
