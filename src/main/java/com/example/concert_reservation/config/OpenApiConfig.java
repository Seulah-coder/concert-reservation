package com.example.concert_reservation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * 
 * Provides API documentation accessible at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI concertReservationOpenAPI() {
        Server localServer = new Server()
            .url("http://localhost:8080")
            .description("Local Development Server");

        Contact contact = new Contact()
            .name("Concert Reservation API Team")
            .email("support@concertreservation.com");

        License license = new License()
            .name("Apache 2.0")
            .url("https://www.apache.org/licenses/LICENSE-2.0.html");

        Info info = new Info()
            .title("Concert Reservation System API")
            .version("1.0.0")
            .description("""
                # Concert Reservation System
                
                A comprehensive concert ticket reservation system with the following features:
                
                ## Core Features
                - 🎫 **Reservations**: Reserve concert seats with automatic expiration
                - 💳 **Payments**: Process payments with balance management
                - 💰 **Balance**: User balance charging and refunds
                - 🔄 **Refunds**: Full refund processing with seat release
                - 🎵 **Concerts**: Browse available concerts and dates
                - 🪑 **Seats**: View and select available seats
                - 🚦 **Queue**: Token-based queue system for high-demand concerts
                
                ## System Design
                - Pessimistic locking for concurrency control
                - Automatic reservation expiration (5 minutes)
                - Transaction-based consistency
                - Tested with 1000+ concurrent users
                
                ## Getting Started
                1. Create a user balance account
                2. Charge your balance
                3. Issue a queue token (if required)
                4. Browse concerts and available dates
                5. Select and reserve a seat
                6. Complete payment within 5 minutes
                7. Refund if needed
                
                ## Notes
                - All monetary amounts are in KRW (Korean Won)
                - Reservations expire after 5 minutes if not paid
                - Queue tokens are required during peak times
                """)
            .contact(contact)
            .license(license);

        return new OpenAPI()
            .info(info)
            .servers(List.of(localServer));
    }
}
