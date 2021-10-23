package com.itechart.book_library.dao.api;

import com.itechart.book_library.model.entity.AuthorEntity;

import java.util.List;
import java.util.Optional;

public interface AuthorDao extends Dao<AuthorEntity> {
    Optional<AuthorEntity> getByName(String name);

    List<AuthorEntity> getByBookId(int id);
}
