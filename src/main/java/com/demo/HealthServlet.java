package com.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class HealthServlet extends HttpServlet {

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
                    "status": "UP",
                    "application": "demoproject"
                }
                """);
    }
}

