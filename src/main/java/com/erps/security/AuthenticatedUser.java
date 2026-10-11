package com.erps.security;

import com.erps.entity.Role;
import com.erps.entity.User;
import com.erps.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    public AuthenticatedUser(UserRepository userRepository, AuthenticationContext authenticationContext) {
        this.userRepository = userRepository;
        this.authenticationContext = authenticationContext;
    }

    public Optional<User> get() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()));
    }

    public boolean hasRole(Role role) {
        return get().map(u -> u.getRole() == role).orElse(false);
    }

    public boolean hasAnyRole(Role... roles) {
        return get().map(u -> {
            for (Role r : roles) {
                if (u.getRole() == r) return true;
            }
            return false;
        }).orElse(false);
    }

    public void logout() {
        authenticationContext.logout();
    }
}
