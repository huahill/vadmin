package io.github.huahill.vadmin.app;

import com.vaadin.flow.spring.annotation.EnableVaadin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication(scanBasePackages = "io.github.huahill.vadmin")
@EnableVaadin({
        "io.github.huahill.vadmin.app",
        "io.github.huahill.vadmin"
})
@EntityScan(basePackages = "io.github.huahill.vadmin.springjpa")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
