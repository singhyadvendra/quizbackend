package com.quiz.dicom.service;

import com.quiz.dicom.domain.AppUser;
import com.quiz.dicom.domain.UserIdentity;
import com.quiz.dicom.dto.IdentityDto;
import com.quiz.dicom.dto.MeDto;
import com.quiz.dicom.repository.AppUserRepository;
import com.quiz.dicom.repository.UserIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeService {

    private final AppUserRepository appUserRepository;
    private final UserIdentityRepository userIdentityRepository;

    public MeService(AppUserRepository appUserRepository, UserIdentityRepository userIdentityRepository) {
        this.appUserRepository = appUserRepository;
        this.userIdentityRepository = userIdentityRepository;
    }

    @Transactional(readOnly = true)
    public MeDto getMe(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<UserIdentity> identities = userIdentityRepository.findAllByUserId(userId);

        List<IdentityDto> identityDtos = identities.stream()
                .map(i -> new IdentityDto(
                        i.getProvider(),
                        i.getProviderSubject(),
                        i.getDisplayName(),
                        i.getEmail(),
                        i.getEmailVerified(),
                        i.getPictureUrl(),
                        i.getLastLoginAt()
                ))
                .toList();

        return new MeDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                identityDtos
        );
    }
}
