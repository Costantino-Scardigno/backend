package com.gestioneFoto.payload.response;

import com.gestioneFoto.model.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImage; // Base64 encoded image

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.profileImage = user.getProfileImage(); // Assuming you'll add this field to User
    }
}