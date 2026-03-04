package com.awsSecretProject;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class MyRestController {
@GetMapping("/")
public String welcome() {
    return new String("<h1>Hello World</h1>");
}

}
