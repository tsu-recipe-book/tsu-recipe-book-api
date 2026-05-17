package ru.nu1ts.recipebook.api;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import ru.nu1ts.recipebook.model.entity.Dish;
import ru.nu1ts.recipebook.model.entity.DishIngredient;
import ru.nu1ts.recipebook.model.entity.Product;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.DishCategory;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.repository.DishRepository;
import ru.nu1ts.recipebook.repository.ProductRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DisplayName("API Tests: Product Management (CRUD)")
public class ProductApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishRepository dishRepository;

    private Product testProduct;

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

    @DisplayName("Creation: Checking the validity of the amount of BJU")
    @ParameterizedTest(name = "B={0}, F={1}, U={2} -> Expected status: {3}")
    @CsvSource({
            "0.0, 0.0, 0.0, 201",       // BVA: Min. border
            "50.0, 25.0, 20.0, 201",    // EP: Valid class
            "33.3, 33.3, 33.4, 201",    // BVA: Upper limit is exactly 100.0
            "33.4, 33.4, 33.3, 422",    // BVA: Out of Bounds (100.1) -> Error 422
            "150.0, 0.0, 0.0, 400",     // EP: Exceeded the limit of 100 on one element
            "-0.1, 10.0, 10.0, 400"     // BVA: Value less than 0
    })
    void createProduct_BjuSumValidation(String proteins, String fats, String carbs, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");

        request.param("name", "Test product BJU")
                .param("calories", "150.0")
                .param("proteins", proteins)
                .param("fats", fats)
                .param("carbohydrates", carbs)
                .param("category", "MEAT")
                .param("cookingRequired", "READY_TO_EAT")
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @DisplayName("Creation: BVA checking for product name length")
    @ParameterizedTest(name = "Name length={0}, value=''{1}'' -> Expected status: {2}")
    @CsvSource({
            "1, A, 400",     // BVA: Less than minimum bound
            "2, AB, 201",    // BVA: Right on the border
            "6, Tomato, 201" // EP: Normal valid value
    })
    void createProduct_NameLengthValidation(int length, String name, int expectedStatus) throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");

        request.param("name", name)
                .param("calories", "100.0")
                .param("proteins", "10.0")
                .param("fats", "5.0")
                .param("carbohydrates", "15.0")
                .param("category", "VEGETABLES")
                .param("cookingRequired", "READY_TO_EAT")
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("Creation: BVA checking for max photos (limit is 5)")
    void createProduct_MaxPhotosLimitExceeded() throws Exception {
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/products");
        request.param("name", "A product with a bunch of photos")
                .param("calories", "10.0").param("proteins", "1.0")
                .param("fats", "1.0").param("carbohydrates", "1.0")
                .param("category", "VEGETABLES")
                .param("cookingRequired", "READY_TO_EAT");

        for (int i = 0; i < 6; i++) {
            request.file(new org.springframework.mock.web.MockMultipartFile(
                    "photos",
                    "photo" + i + ".jpg",
                    "image/jpeg",
                    "fake image data".getBytes()
            ));
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("Reading: Getting a product by existing ID")
    void getProductById_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basic product"))
                .andExpect(jsonPath("$.calories").value(100.0));
    }

    @Test
    @DisplayName("Read: Trying to get a product by a non-existent ID returns 404")
    void getProductById_NotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("Reading: Getting a list of products with filtering (Search by substring)")
    void getProductsList_WithSearchFilter() throws Exception {
        Product ignoreProduct = Product.builder()
                .name("Completely different")
                .calories(0.0).proteins(0.0).fats(0.0).carbohydrates(0.0)
                .category(ProductCategory.MEAT).cookingRequired(CookingRequired.READY_TO_EAT)
                .build();
        productRepository.save(ignoreProduct);

        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                        .param("search", "Basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic product"));
    }

    @Test
    @DisplayName("Reading: Filter products by category and flags")
    void getProductsList_WithCategoryAndFlagFilters() throws Exception {
        testProduct.getFlags().add(ProductFlag.VEGAN);
        productRepository.save(testProduct);

        Product meatProduct = Product.builder()
                .name("Meat")
                .calories(200.0).proteins(20.0).fats(10.0).carbohydrates(0.0)
                .category(ProductCategory.MEAT)
                .cookingRequired(CookingRequired.REQUIRES_COOKING)
                .build();
        productRepository.save(meatProduct);

        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                        .param("category", "VEGETABLES")
                        .param("flags", "VEGAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic product"));
    }

    @Test
    @DisplayName("Reading: Sorting products by calories descending")
    void getProductsList_SortedByCalories() throws Exception {
        Product highCalProduct = Product.builder()
                .name("High Calorie Product")
                .calories(500.0).proteins(10.0).fats(10.0).carbohydrates(10.0)
                .category(ProductCategory.MEAT)
                .cookingRequired(CookingRequired.READY_TO_EAT)
                .build();
        productRepository.save(highCalProduct);

        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                        .param("sortBy", "calories")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("High Calorie Product"))
                .andExpect(jsonPath("$[1].name").value("Basic product"));
    }

    @Test
    @DisplayName("Update: Successfully updated product attributes")
    void updateProduct_Success() throws Exception {
        MockMultipartHttpServletRequestBuilder updateReq = MockMvcRequestBuilders.multipart("/products/" + testProduct.getId());
        updateReq.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        updateReq.param("name", "Updated product")
                .param("calories", "150.0")
                .param("proteins", "10.0")
                .param("fats", "5.0")
                .param("carbohydrates", "15.0")
                .param("category", "MEAT")
                .param("cookingRequired", "READY_TO_EAT")
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(updateReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated product"))
                .andExpect(jsonPath("$.category").value("MEAT"));
    }

    @Test
    @DisplayName("Update: EP checking validation works on update (invalid BJU sum)")
    void updateProduct_InvalidBjuSum_Returns422() throws Exception {
        MockMultipartHttpServletRequestBuilder updateReq =
                MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT, "/products/" + testProduct.getId());

        updateReq.param("name", "Updated")
                .param("calories", "100.0")
                .param("proteins", "50.0")
                .param("fats", "50.0")
                .param("carbohydrates", "50.0")
                .param("category", "MEAT")
                .param("cookingRequired", "READY_TO_EAT")
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(updateReq)
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Deletion: Successfully deleted a product without links")
    void deleteProduct_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/products/" + testProduct.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Removal: Error 409 (Conflict) if product is used in a dish")
    void deleteProduct_ConflictWhenUsedInDish() throws Exception {
        Dish testDish = Dish.builder()
                .name("Salad with a base product")
                .calories(100.0).proteins(10.0).fats(5.0).carbohydrates(15.0).portionSize(200.0)
                .category(DishCategory.SALAD)
                .build();

        DishIngredient ingredient = DishIngredient.builder()
                .product(testProduct)
                .weight(100.0)
                .build();
        testDish.addIngredient(ingredient);
        dishRepository.save(testDish);

        mockMvc.perform(MockMvcRequestBuilders.delete("/products/" + testProduct.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.usedInDishes[0].name").value("Salad with a base product"));
    }
}
