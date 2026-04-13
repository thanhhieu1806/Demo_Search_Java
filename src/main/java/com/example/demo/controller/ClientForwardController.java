package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientForwardController {

    @GetMapping(value = {
            "/",
            "/{path:^(?!api$|static$|logout$|error$|h2-console$)[^.]+$}",
            "/{path:^(?!api$|static$|logout$|error$|h2-console$)[^.]+$}/{*remaining}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
