package com.gestioneFoto.payload.request;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class UserUpdateRequest {
    @Email(message = "Formato email non valido")
    private String email;

    @Size(min = 6, message = "La nuova password deve contenere almeno 6 caratteri")
    private String newPassword;

    @Size(min = 6, message = "La password attuale deve essere inserita")
    private String currentPassword;

    @Size(max = 10 * 1024 * 1024, message = "L'immagine non può superare 10MB")
    private String profileImage; // Base64 encoded image
}