package com.GIU26B.KronoCash;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class KronoCashApplication {

    /** Zona horaria de México para LocalDateTime.now() y DEFAULT en la JVM. */
    @PostConstruct
    public void initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
    }

    public static void main(String[] args) {
        SpringApplication.run(KronoCashApplication.class, args);
    }

}
