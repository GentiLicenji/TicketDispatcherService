package com.pleased.ticket.dispatcher.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;


@SpringBootApplication
@ComponentScan(basePackages = {
        "com.pleased.ticket.dispatcher.server.controller",
        "com.pleased.ticket.dispatcher.server.delegate",
        "com.pleased.ticket.dispatcher.server.service",
        "com.pleased.ticket.dispatcher.server.exception",
        "com.pleased.ticket.dispatcher.server.util",
        "com.pleased.ticket.dispatcher.server.config"})
@EntityScan("com.pleased.ticket.dispatcher.server.model.db")
@EnableR2dbcRepositories("com.pleased.ticket.dispatcher.server.repository")
public class AppStarter{

    public static void main(String[] args) {
        SpringApplication.run(AppStarter.class, args);
    }

}
