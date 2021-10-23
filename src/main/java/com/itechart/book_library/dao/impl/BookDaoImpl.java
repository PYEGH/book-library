package com.itechart.book_library.dao.impl;

import com.itechart.book_library.dao.api.BaseDao;
import com.itechart.book_library.dao.api.BookDao;
import com.itechart.book_library.dao.criteria.BookSpecification;
import com.itechart.book_library.model.entity.AuthorEntity;
import com.itechart.book_library.model.entity.BookEntity;
import com.itechart.book_library.model.entity.GenreEntity;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BookDaoImpl extends BaseDao implements BookDao {

    private static final Logger log = Logger.getLogger(BookDaoImpl.class);

    private static final String INSERT_BOOK_QUERY = "INSERT INTO book (id, title, publisher, publish_date, page_count, isbn, description, cover, available, total_amount) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM book";
    private static final String SELECT_LIMIT_OFFSET_WITH_PARAMETERS_QUERY = """
            with parameters(title_p, authors_p, genres_p, description_p) as (
                values (?, ?, ?, ?)
            )
            select book.*, author.*, genre.*
            from parameters, book
                     left join author_book on author_book.book_id = book.id
                     left join author on author_book.author_id = author.id
                     left join genre_book on genre_book.book_id = book.id
                     left join genre on genre_book.genre_id = genre.id
            WHERE title ~* title_p
              and author.name ~* authors_p
              and genre.name ~* genres_p
              and description ~* description_p
              and book.id in (select distinct book.id
                              from parameters, book
                                       left join author_book on author_book.book_id = book.id
                                       left join author on author_book.author_id = author.id
                                       left join genre_book on genre_book.book_id = book.id
                                       left join genre on genre_book.genre_id = genre.id
                              WHERE title ~* title_p
                                and author.name ~* authors_p
                                and genre.name ~* genres_p
                                and description ~* description_p
                              order by book.id desc
                              limit ? offset ?);""";
    private static final String SELECT_LIMIT_OFFSET_QUERY = """
            select book.*, author.*, genre.*
            from book
                     left join author_book on author_book.book_id = book.id
                     left join author on author_book.author_id = author.id
                     left join genre_book on genre_book.book_id = book.id
                     left join genre on genre_book.genre_id = genre.id
            where book.id in
            (select book.id from book order by book.id desc limit ? offset ?)""";
    private static final String SELECT_BY_ID_QUERY = """
            select book.*, author.*, genre.*
            from book
                     left join author_book on author_book.book_id = book.id
                     left join author on author_book.author_id = author.id
                     left join genre_book on genre_book.book_id = book.id
                     left join genre on genre_book.genre_id = genre.id
            where book.id = ?""";
    private static final String UPDATE_QUERY = "UPDATE book SET title = ?, publisher = ?, publish_date = ?, page_count = ?, isbn = ?, description = ?, cover = COALESCE(?, cover), available = ? - total_amount + available, total_amount = ? WHERE id = ?";
    private static final StringBuilder TEMPLATE_DELETE_QUERY = new StringBuilder("DELETE FROM book WHERE id IN(?");
    private static StringBuilder DELETE_QUERY = TEMPLATE_DELETE_QUERY;
    private static final String SELECT_BOOK_COUNT = "SELECT COUNT(*) FROM book";
    private static final String SELECT_BOOK_COUNT_WITH_PARAMETERS = """
            select count(DISTINCT book.id) from
            book
                     left join author_book on author_book.book_id = book.id
                     left join author on author_book.author_id = author.id
                     left join genre_book on genre_book.book_id = book.id
                     left join genre on genre_book.genre_id = genre.id
            WHERE title ~* ?
              and author.name ~* ?
              and genre.name ~* ?
              and description ~* ?;""";
    private static final String UPDATE_TAKE_BOOK_QUERY = "UPDATE book SET available = available-1 WHERE id = ?";
    private static final String SELECT_BY_TITLE_QUERY = "SELECT * FROM book WHERE title = ?";
    private static final String SELECT_BY_AUTHOR_QUERY = "SELECT * FROM book JOIN author_book ON book.id = author_book.author_id WHERE author_id = ?";
    private static final String SELECT_BY_GENRE_QUERY = "SELECT * FROM book JOIN genre_book ON book.id = genre_book.genre_id WHERE genre_id = ?";
    private static final String SELECT_BY_DESCRIPTION_QUERY = "SELECT * FROM book WHERE description LIKE '%?%'";

    @Override
    public BookEntity create(BookEntity book) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_BOOK_QUERY)) {
            int i = 1;
            statement.setString(i++, book.getTitle());
            statement.setString(i++, book.getPublisher());
            statement.setDate(i++, book.getPublishDate());
            statement.setInt(i++, book.getPageCount());
            statement.setString(i++, book.getISBN());
            statement.setString(i++, book.getDescription());
            statement.setBinaryStream(i++, book.getCover());
            statement.setInt(i++, book.getAvailableBookAmount());
            statement.setInt(i++, book.getTotalBookAmount());
            statement.execute();
            book.setId(getIdAfterInserting(statement));
        } catch (SQLException e) {
            log.error("Cannot create book ", e);
        } finally {
            connectionPool.returnToPool(connection);
        }
        return book;
    }

    @Override
    public List<BookEntity> getLimitOffset(int limit, int offset) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LIMIT_OFFSET_QUERY)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            return getBookListFromResultSet(statement.executeQuery());
        } catch (SQLException e) {
            log.error("Cannot get books ", e);
            return null;
        } finally {
            connectionPool.returnToPool(connection);
        }
    }

    @Override
    public List<BookEntity> getLimitOffsetBySpecification(BookSpecification specification, int limit, int offset) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LIMIT_OFFSET_WITH_PARAMETERS_QUERY)) {
            int i = 1;
            statement.setString(i++, specification.getTitle());
            statement.setString(i++, specification.getAuthors());
            statement.setString(i++, specification.getGenres());
            statement.setString(i++, specification.getDescription());
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            return getBookListFromResultSet(statement.executeQuery());
        } catch (SQLException e) {
            log.error("Cannot get books ", e);
            return null;
        } finally {
            connectionPool.returnToPool(connection);
        }
    }

    @Override
    public Optional<BookEntity> getById(int id) {
        List<BookEntity> bookList = getListByKey(SELECT_BY_ID_QUERY, id);
        return bookList.isEmpty() ? Optional.empty() : Optional.of(bookList.get(0));
    }

    @Override
    public void update(BookEntity book) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_QUERY)) {
            int i = 1;
            statement.setString(i++, book.getTitle());
            statement.setString(i++, book.getPublisher());
            statement.setDate(i++, book.getPublishDate());
            statement.setInt(i++, book.getPageCount());
            statement.setString(i++, book.getISBN());
            statement.setString(i++, book.getDescription());
            statement.setBinaryStream(i++, book.getCover());
            statement.setInt(i++, book.getTotalBookAmount());
            statement.setInt(i++, book.getTotalBookAmount());
            statement.setInt(i++, book.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot update book ", e);
        }
        connectionPool.returnToPool(connection);
    }

    @Override
    public void update(BookEntity book, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_QUERY)) {
            int i = 1;
            statement.setString(i++, book.getTitle());
            statement.setString(i++, book.getPublisher());
            statement.setDate(i++, book.getPublishDate());
            statement.setInt(i++, book.getPageCount());
            statement.setString(i++, book.getISBN());
            statement.setString(i++, book.getDescription());
            statement.setBinaryStream(i++, book.getCover());
            statement.setInt(i++, book.getTotalBookAmount());
            statement.setInt(i++, book.getTotalBookAmount());
            statement.setInt(i++, book.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.error(ex);
            }
            log.error("Cannot update book ", e);
        }
    }

    @Override
    public void delete(Integer[] ids) {
        DELETE_QUERY.append(",?".repeat(Math.max(0, ids.length - 1)));
        DELETE_QUERY.append(")");
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(String.valueOf(DELETE_QUERY))) {
            for (int i = 0; i < ids.length; i++) {
                statement.setInt(i + 1, ids[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot delete book ", e);
        } finally {
            connectionPool.returnToPool(connection);
            DELETE_QUERY = TEMPLATE_DELETE_QUERY;
        }
    }

    public int getCount() {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BOOK_COUNT)) {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("Cannot get count of books ", e);
            return 0;
        } finally {
            connectionPool.returnToPool(connection);
            DELETE_QUERY = TEMPLATE_DELETE_QUERY;
        }
    }

    public int getCountBySpecification(BookSpecification specification) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BOOK_COUNT_WITH_PARAMETERS)) {
            statement.setString(1, specification.getTitle());
            statement.setString(2, specification.getAuthors());
            statement.setString(3, specification.getGenres());
            statement.setString(4, specification.getDescription());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("Cannot get count of books ", e);
            return 0;
        } finally {
            connectionPool.returnToPool(connection);
            DELETE_QUERY = TEMPLATE_DELETE_QUERY;
        }
    }

    public void takeBook(int id) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_TAKE_BOOK_QUERY)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot get book ", e);
        } finally {
            connectionPool.returnToPool(connection);
        }
    }

    private List<BookEntity> getListByKey(String query, int id) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            return getBookListFromResultSet(statement.executeQuery());
        } catch (SQLException e) {
            log.error("Cannot get list by " + id + " key ", e);
            return new ArrayList<>();
        } finally {
            connectionPool.returnToPool(connection);
        }
    }

    private List<BookEntity> getListByKey(String query, String text) {
        Connection connection = connectionPool.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, text);
            return getBookListFromResultSet(statement.executeQuery());
        } catch (SQLException e) {
            log.error("Cannot get list by " + text + " key ", e);
            return new ArrayList<>();
        } finally {
            connectionPool.returnToPool(connection);
        }
    }

    private List<BookEntity> getBookListFromResultSet(ResultSet resultSet) throws SQLException {
        int prevBookId = 0;
        if (resultSet.next()) {
            prevBookId = resultSet.getInt(1);
        }

        BookEntity book = getBookWithParams(resultSet);
        List<BookEntity> books = new ArrayList<>();
        Set<AuthorEntity> authorSet = new HashSet<>();
        Set<GenreEntity> genreSet = new HashSet<>();

        do {
            if (prevBookId != resultSet.getInt(1)) {
                book.setAuthorEntities(new ArrayList<>(authorSet));
                book.setGenreEntities(new ArrayList<>(genreSet));
                authorSet.clear();
                genreSet.clear();
                books.add(book);
            }
            AuthorEntity author = new AuthorEntity();
            author.setId(resultSet.getInt(11));
            author.setName(resultSet.getString(12));

            GenreEntity genre = new GenreEntity();
            genre.setId(resultSet.getInt(13));
            genre.setName(resultSet.getString(14));

            genreSet.add(genre);
            authorSet.add(author);
            book = getBookWithParams(resultSet);

            prevBookId = resultSet.getInt(1);
        } while (resultSet.next());
        book.setAuthorEntities(new ArrayList<>(authorSet));
        book.setGenreEntities(new ArrayList<>(genreSet));
        books.add(book);
        return books;
    }

    private BookEntity getBookWithParams(ResultSet resultSet) throws SQLException {
        BookEntity book = new BookEntity();
        int i = 1;
        book.setId(resultSet.getInt(i++));
        book.setTitle(resultSet.getString(i++));
        book.setPublisher(resultSet.getString(i++));
        book.setPublishDate(resultSet.getDate(i++));
        book.setPageCount(resultSet.getInt(i++));
        book.setISBN(resultSet.getString(i++));
        book.setDescription(resultSet.getString(i++));
        book.setCover(resultSet.getBinaryStream(i++));
        book.setAvailableBookAmount(resultSet.getInt(i++));
        book.setTotalBookAmount(resultSet.getInt(i++));
        return book;
    }
}
