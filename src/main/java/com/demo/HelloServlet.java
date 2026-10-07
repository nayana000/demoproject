package com.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding("UTF-8");

        response.setStatus(
                HttpServletResponse.SC_OK
        );

        response.getWriter().write("""
                {
                    "message": "Hello from Java CI/CD Demo!",
                    "status": "success"
                }
                """);
    }
}

