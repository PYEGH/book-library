package com.itechart.book_library.model.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReaderDto {

    int id;
    String email;
    String name;
}
