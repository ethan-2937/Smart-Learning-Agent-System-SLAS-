package com.hlju.learning;

import com.hlju.learning.config.AiProperties;
import com.hlju.learning.config.AuthProperties;
import com.hlju.learning.config.EmbeddingProperties;
import com.hlju.learning.config.StorageProperties;
import com.hlju.learning.config.VectorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.hlju.learning.mapper")
@EnableConfigurationProperties({AiProperties.class, AuthProperties.class, EmbeddingProperties.class, VectorProperties.class, StorageProperties.class})
public class SmartLearningAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartLearningAgentApplication.class, args);
    }
}
