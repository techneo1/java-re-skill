package com.srikanth.javareskill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry-point for the java-re-skill REST API.
 *
 * <p>{@code @SpringBootApplication} is a convenience annotation that combines:</p>
 * <ul>
 *   <li>{@code @Configuration}    – marks this class as a source of bean definitions</li>
 *   <li>{@code @EnableAutoConfiguration} – lets Spring Boot auto-configure the app
 *       based on the jars on the classpath (e.g. Jackson, Tomcat, Validation)</li>
 *   <li>{@code @ComponentScan}    – scans this package and sub-packages for
 *       {@code @Component}, {@code @Service}, {@code @Repository},
 *       {@code @RestController}, etc.</li>
 * </ul>
 *
 * <p>Run with {@code mvn spring-boot:run} or execute the fat-jar produced by
 * {@code mvn package}.</p>
 */
@SpringBootApplication
public class RestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }
}

