package com.sentinel.hemo_grid.auth.persistence;

import java.util.Optional;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

	@Query("select user from AppUser user left join fetch user.organization where lower(user.email) = lower(:email)")
	Optional<AppUser> findByEmailIgnoreCaseWithOrganization(String email);

	@Query("select user from AppUser user left join fetch user.organization where user.id = :id")
	Optional<AppUser> findByIdWithOrganization(UUID id);
}
