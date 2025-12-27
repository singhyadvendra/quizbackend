package com.quiz.dicom.repository;

import com.quiz.dicom.domain.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    List<UserIdentity> findAllByUserId(Long userId);

    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
}
