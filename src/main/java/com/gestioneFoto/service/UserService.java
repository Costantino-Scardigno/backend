package com.gestioneFoto.service;

import com.gestioneFoto.model.User;
import com.gestioneFoto.payload.request.UserUpdateRequest;
import com.gestioneFoto.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest updateRequest) {
        // Find the user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Update email if provided
        if (updateRequest.getEmail() != null) {
            user.setEmail(updateRequest.getEmail());
        }

        // Update profile image if provided
        if (updateRequest.getProfileImage() != null) {
            user.setProfileImage(updateRequest.getProfileImage());
            return userRepository.save(user);
        }

        // Handle password change
        if (updateRequest.getNewPassword() != null) {
            // Verify current password
            if (!passwordEncoder.matches(updateRequest.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Password attuale non corretta");
            }

            // Encode and set new password
            user.setPassword(passwordEncoder.encode(updateRequest.getNewPassword()));
        }

        // Save and return updated user
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}