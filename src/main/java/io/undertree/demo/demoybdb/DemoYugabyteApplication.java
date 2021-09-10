package io.undertree.demo.demoybdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class DemoYugabyteApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoYugabyteApplication.class, args);
    }

}
