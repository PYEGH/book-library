package com.itechart.book_library.action.api;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Action {

    ActionResult execute(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
