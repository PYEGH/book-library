package com.itechart.book_library.action.post;

import com.itechart.book_library.action.api.Action;
import com.itechart.book_library.action.api.ActionConstants;
import com.itechart.book_library.action.api.ActionResult;
import com.itechart.book_library.service.BookService;
import com.itechart.book_library.util.converter.api.BookConverter;
import com.itechart.book_library.util.converter.impl.BookConverterImpl;
import com.itechart.book_library.util.validator.BookFormValidator;
import lombok.extern.log4j.Log4j;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j
public class BookAddAction implements Action {

    BookService bookService = BookService.INSTANCE;
    private final BookConverter bookConverter = new BookConverterImpl();
    private final BookFormValidator bookValidator = BookFormValidator.INSTANCE;

    @Override
    public ActionResult execute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (bookValidator.isValid(req)) {
            bookService.create(bookConverter.toDtoFromReq(req));
            return new ActionResult(ActionConstants.BOOK_LIST_PAGE, ActionConstants.redirect);
        } else {
            log.warn("Invalid book parameters caught");
            return new ActionResult(ActionConstants.BOOK_ADD_PAGE, ActionConstants.redirect);
        }
    }
}
