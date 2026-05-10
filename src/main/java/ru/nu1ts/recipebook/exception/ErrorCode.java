package ru.nu1ts.recipebook.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(400),
    NOT_FOUND(404),
    BJU_SUM_EXCEEDED(422),
    PRODUCT_IN_USE(409),
    FLAG_NOT_AVAILABLE(422),
    INTERNAL_ERROR(500);

    private final int code;
}