package com.gestioneFoto.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gestioneFoto.model.User;
import com.gestioneFoto.payload.request.UserUpdateRequest;
import com.gestioneFoto.payload.response.MessageResponse;
import com.gestioneFoto.payload.response.UserResponse;
import com.gestioneFoto.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final Cloudinary cloudinary;

    public UserController(UserService userService, Cloudinary cloudinary) {
        this.userService = userService;
        this.cloudinary = cloudinary;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestPart(value = "userData", required = false) UserUpdateRequest updateRequest,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        try {
            // Se updateRequest non è fornito, crea un nuovo oggetto vuoto
            UserUpdateRequest finalRequest = updateRequest != null ? updateRequest : new UserUpdateRequest();

            // Se è fornita un'immagine, caricala su Cloudinary
            if (profileImage != null && !profileImage.isEmpty()) {
                // Verifica il tipo di file
                String contentType = profileImage.getContentType();
                if (contentType == null || (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/gif"))) {
                    return ResponseEntity.badRequest().body(new MessageResponse("Formato immagine non supportato. Usa JPEG, PNG o GIF"));
                }

                // Verifica dimensione file (max 10MB)
                if (profileImage.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(new MessageResponse("La dimensione dell'immagine non deve superare 10MB"));
                }

                // Carica l'immagine su Cloudinary
                @SuppressWarnings("unchecked")
                Map<String, String> uploadResult = cloudinary.uploader().upload(
                        profileImage.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "profile_images",
                                "resource_type", "image"
                        )
                );

                // Ottieni l'URL dell'immagine e impostalo nella richiesta di aggiornamento
                String imageUrl = uploadResult.get("url");
                finalRequest.setProfileImage(imageUrl);
            }

            // Aggiorna l'utente con i dati forniti
            User updatedUser = userService.updateUser(id, finalRequest);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Errore nel caricamento dell'immagine: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Errore nell'aggiornamento dell'utente: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully."));
    }
}