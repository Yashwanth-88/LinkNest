package com.linknest.linknest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

@Configuration
@EnableJpaRepositories(repositoryBaseClass = SimpleJpaRepository.class)
public class RepositoryConfig {
    // No additional configuration needed if naming convention is followed
}