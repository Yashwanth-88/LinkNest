package com.linknest.linknest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Role;
import com.linknest.linknest.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.linknest.linknest.repository")
public class LinkNestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkNestApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("newuser").isEmpty()) {
				User user = new User();
				user.setUsername("newuser");
				user.setPassword(passwordEncoder.encode("newpass123"));
				Set<Role> roles = new HashSet<>();
				roles.add(Role.USER);
				user.setRoles(roles);
				userRepository.save(user);
				System.out.println("Initialized user: newuser with ROLE_USER");
			}
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("adminpass123"));
				Set<Role> roles = new HashSet<>();
				roles.add(Role.ADMIN);
				admin.setRoles(roles);
				userRepository.save(admin);
				System.out.println("Initialized admin: admin with ROLE_ADMIN");
			}
		};
	}

//	 Removed to avoid conflict with SecurityConfig
//	 @Bean
//	 public BCryptPasswordEncoder passwordEncoder() {
//	     return new BCryptPasswordEncoder();
//	 }
}