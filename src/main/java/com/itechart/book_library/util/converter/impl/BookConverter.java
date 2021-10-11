package com.itechart.book_library.util.converter.impl;

import com.itechart.book_library.model.dto.BookDto;
import com.itechart.book_library.model.entity.BookEntity;
import com.itechart.book_library.util.converter.Converter;


public class BookConverter extends Converter<BookDto, BookEntity> {

    public BookConverter() {
        super(BookConverter::convertToEntity, BookConverter::convertToDto);
    }

    private static BookDto convertToDto(BookEntity bookEntity) {
        return new BookDto(
                bookEntity.getId(),
                bookEntity.getTitle(),
                bookEntity.getPublisher(),
                bookEntity.getPublishDate(),
                bookEntity.getPageCount(),
                bookEntity.getISBN(),
                bookEntity.getDescription(),
                bookEntity.getCover());
    }

    private static BookEntity convertToEntity(BookDto bookDto) {
        return new BookEntity(
                bookDto.getId(),
                bookDto.getTitle(),
                bookDto.getPublisher(),
                bookDto.getPublishDate(),
                bookDto.getPageCount(),
                bookDto.getISBN(),
                bookDto.getDescription(),
                bookDto.getCover());
    }


}