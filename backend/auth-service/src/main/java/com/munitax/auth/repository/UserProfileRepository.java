package com.munitax.auth.repository;

import com.munitax.auth.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    List<UserProfile> findByUserIdAndActive(String userId, boolean active);

    Optional<UserProfile> findByUserIdAndIsPrimaryAndActive(String userId, boolean isPrimary, boolean active);

    List<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByIdAndUserId(String id, String userId);
}
