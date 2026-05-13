package ru.nu1ts.recipebook.model.entity;

import java.util.List;

public interface PhotoManaged<T> {
    List<T> getPhotos();
    void addPhoto(T photo);
}
