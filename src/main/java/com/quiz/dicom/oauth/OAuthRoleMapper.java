package com.quiz.dicom.oauth;

import com.quiz.dicom.config.AppAdminProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OAuthRoleMapper {

    private final Set<String> adminEmails;

    public OAuthRoleMapper(AppAdminProperties props) {
        this.adminEmails = Set.copyOf(props.getEmails());
    }

    public Set<GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> existing,
            String email
    ) {
        Set<GrantedAuthority> mapped = new HashSet<>(existing);
        mapped.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (email != null && adminEmails.contains(email.toLowerCase())) {
            mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return mapped;
    }
}
