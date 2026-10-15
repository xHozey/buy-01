package com.user.userService.reposiroty;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.user.userService.model.User;

public interface  UserRepository extends MongoRepository<String, User>{
    
}
