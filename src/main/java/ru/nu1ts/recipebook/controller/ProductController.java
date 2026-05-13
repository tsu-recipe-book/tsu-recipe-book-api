package ru.nu1ts.recipebook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nu1ts.recipebook.dto.ProductCreateRequest;
import ru.nu1ts.recipebook.dto.ProductDto;
import ru.nu1ts.recipebook.dto.ProductListItem;
import ru.nu1ts.recipebook.dto.ProductUpdateRequest;
import ru.nu1ts.recipebook.model.enums.CookingRequired;
import ru.nu1ts.recipebook.model.enums.ProductCategory;
import ru.nu1ts.recipebook.model.enums.ProductFlag;
import ru.nu1ts.recipebook.service.ProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Управление продуктами")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Получить список продуктов", description = "Возвращает список всех продуктов с поддержкой фильтрации, поиска и сортировки.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список продуктов успешно получен"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<ProductListItem>> getProducts(
            @Parameter(description = "Поиск по подстроке в названии") @RequestParam(required = false) String search,
            @Parameter(description = "Фильтр по категории") @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "Фильтр по готовности") @RequestParam(required = false) CookingRequired cookingRequired,
            @Parameter(description = "Фильтр по флагам (должен иметь все указанные)") @RequestParam(required = false) List<ProductFlag> flags,
            @Parameter(description = "Поле для сортировки") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Направление сортировки") @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        return ResponseEntity.ok(productService.getProducts(search, category, cookingRequired, flags, sortBy, sortOrder));
    }

    @Operation(summary = "Получить продукт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт найден"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден", content = @Content)
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @Operation(summary = "Создать новый продукт", description = "Создание продукта с загрузкой фотографий через multipart/form-data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Продукт успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> createProduct(@ModelAttribute @Valid ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @Operation(summary = "Обновить продукт", description = "Полное обновление продукта. Старые фото заменяются новыми, если они переданы.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт успешно обновлен"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден", content = @Content),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content)
    })
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID productId,
            @ModelAttribute @Valid ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @Operation(summary = "Удалить продукт")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Продукт успешно удален"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден", content = @Content)
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
