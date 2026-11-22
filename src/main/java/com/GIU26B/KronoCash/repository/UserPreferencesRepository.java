package com.GIU26B.KronoCash.repository;

import com.GIU26B.KronoCash.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** La PK de UserPreferences es user_id, por lo que el ID es el del usuario. */
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);
}
