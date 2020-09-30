package ib.project.repo;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ib.project.model.User;

public interface UserRepository  extends JpaRepository<User, Long> {
	 Optional<User> findByEmail(String email);
	 List<User> findAll();
	 List<User> findByActiveTrue();
	 List<User> findByActiveFalse();
}
