package com.example.demo.controller;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.demo.model.User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Greeting {
    private static final String template = "Mr/Mrs %s";
    private final AtomicInteger lastID = new AtomicInteger();

    @GetMapping("/greeting")
    public User greetingMethod(
        @RequestParam(name = "name",defaultValue = "Long") String userName){
        return new User(lastID.incrementAndGet(),String.format(template, userName));
    }
}