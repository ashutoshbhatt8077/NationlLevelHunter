package devpiolet.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import devpiolet.backend.entity.User;
import java.util.List;
import  java.util.Optional;

public interface UserRepository extends JpaRepository<User,UUID>{

    Optional<User> findByGhId(Long ghId);
} 
