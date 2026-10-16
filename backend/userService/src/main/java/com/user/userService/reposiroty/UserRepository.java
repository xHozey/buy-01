package com.user.userService.reposiroty;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.user.userService.model.User;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
}
