package com.itechart.book_library.model.dto;

public class AuthorDto {

    private final String name;

    public AuthorDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
