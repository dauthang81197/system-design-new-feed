package com.pv.posts;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "jwt.access-secret=test_secret_at_least_32_bytes_long",
    "s3.endpoint=http://localhost:9000",
    "s3.access-key=minioadmin",
    "s3.secret-key=minioadmin",
    "s3.region=us-east-1",
    "s3.bucket=posts-media"
})
class PostsApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts correctly
    }
}

