package com.gestioneFoto.controller;

import com.gestioneFoto.model.Event;
import com.gestioneFoto.model.User;
import com.gestioneFoto.payload.request.EventRequest;
import com.gestioneFoto.payload.response.EventResponse;
import com.gestioneFoto.payload.response.MessageResponse;
import com.gestioneFoto.payload.response.ShareLinkResponse;
import com.gestioneFoto.service.EventService;
import com.gestioneFoto.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest eventRequest, Authentication authentication) {
        String username=authentication.getName();
        User creator = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setDescription(eventRequest.getDescription());
        event.setEventDate(eventRequest.getEventDate());
        event.setCreatedBy(creator);

        // Generiamo un codice univoco di condivisione per ogni nuovo evento
        event.setShareCode(UUID.randomUUID().toString());

        Event savedEvent = eventService.createEvent(event);
        EventResponse eventResponse=eventService.convertEventToDto(event);
        return ResponseEntity.ok(eventResponse);
    }

    // Endpoint per generare un link di condivisione
    @PostMapping("/{id}/share")
    public ResponseEntity<?> generateShareLink(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Evento non trovato"));

        // Verifica che l'utente sia il creatore dell'evento
        if (event.getCreatedBy() == null ||
                !event.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(
                    new MessageResponse("Non hai accesso a questo evento"));
        }

        // Se l'evento non ha già un codice di condivisione, ne generiamo uno
        if (event.getShareCode() == null || event.getShareCode().isEmpty()) {
            event.setShareCode(UUID.randomUUID().toString());
            event = eventService.updateEvent(event);
        }

        // Genera URL di condivisione e risposta
        String shareUrl = "/album/share/" + event.getShareCode();

        return ResponseEntity.ok(new ShareLinkResponse(
                event.getShareCode(),
                shareUrl,
                event.getName()
        ));
    }

    // Endpoint pubblico per accedere a un album condiviso tramite codice
    @GetMapping("/share/{shareCode}")
    public ResponseEntity<?> getSharedEvent(@PathVariable String shareCode,
                                            Authentication authentication) {
        Event event = eventService.getEventByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Evento condiviso non trovato"));

        // Se l'utente è autenticato, aggiungiamo automaticamente l'evento alla sua lista di condivisi
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato"));

            // Non aggiungiamo l'evento se l'utente è il creatore
            if (event.getCreatedBy() == null || !event.getCreatedBy().getId().equals(user.getId())) {
                // Aggiungiamo l'album alla lista degli album condivisi con l'utente
                eventService.shareEventWithUser(event.getId(), user.getId());
            }
        }

        // Per un evento condiviso, possiamo ritornare dettagli pubblici
        EventResponse eventResponse = eventService.convertEventToDtoWithDetails(event);

        return ResponseEntity.ok(eventResponse);
    }

    @PostMapping("/{id}/share/user/{userId}")
    public ResponseEntity<?> shareEventWithUser(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Evento non trovato"));

        // Verifica che l'utente sia il creatore dell'evento
        if (event.getCreatedBy() == null ||
                !event.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(
                    new MessageResponse("Non hai accesso a questo evento"));
        }

        // Condividi l'evento con l'utente specificato
        eventService.shareEventWithUser(id, userId);

        return ResponseEntity.ok(new MessageResponse("Album condiviso con successo"));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<EventResponse>> getSharedEvents(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean includeDetails) {

        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato!!"));

        // Recupera gli eventi condivisi con l'utente
        List<Event> sharedEvents = eventService.getSharedEventsByUser(user);

        // Usa il convertitore appropriato
        List<EventResponse> eventResponses;
        if (includeDetails) {
            eventResponses = sharedEvents.stream()
                    .map(eventService::convertEventToDtoWithDetails)
                    .toList();
        } else {
            eventResponses = sharedEvents.stream()
                    .map(eventService::convertEventToDto)
                    .toList();
        }

        return ResponseEntity.ok(eventResponses);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean includeDetails) {

        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato!!"));

        List<Event> events = eventService.getEventsByUser(user);

        // Usa il convertitore appropriato in base al parametro includeDetails
        List<EventResponse> eventResponses;
        if (includeDetails) {
            eventResponses = events.stream()
                    .map(eventService::convertEventToDtoWithDetails)
                    .toList();
        } else {
            eventResponses = events.stream()
                    .map(eventService::convertEventToDto)
                    .toList();
        }

        return ResponseEntity.ok(eventResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id,
                                          @RequestParam(defaultValue = "false") boolean includeDetails,
                                          Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato!!"));

        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Evento non trovato!!"));

        // Verifica che l'utente sia il creatore dell'evento o che l'evento sia condiviso con lui
        if ((event.getCreatedBy() == null || !event.getCreatedBy().getId().equals(user.getId()))
                && (event.getSharedWithUserIds() == null || !event.getSharedWithUserIds().contains(user.getId()))) {
            return ResponseEntity.status(403).body(
                    new MessageResponse("Non hai accesso a questo evento"));
        }

        EventResponse eventResponse;
        if (includeDetails) {
            eventResponse = eventService.convertEventToDtoWithDetails(event);
        } else {
            eventResponse = eventService.convertEventToDto(event);
        }

        return ResponseEntity.ok(eventResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest eventRequest,Authentication authentication) {
        String username= authentication.getName();
        User creator =userService.findByUsername(username).orElseThrow();

        // Otteniamo l'evento esistente per mantenere il codice di condivisione
        Event existingEvent = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Evento non trovato!"));

        // Verifichiamo che l'utente sia il creatore dell'evento
        if (existingEvent.getCreatedBy() == null ||
                !existingEvent.getCreatedBy().getId().equals(creator.getId())) {
            return ResponseEntity.status(403).body(
                    new MessageResponse("Non hai il permesso di modificare questo evento"));
        }

        Event event = new Event();
        event.setId(id);
        event.setName(eventRequest.getName());
        event.setDescription(eventRequest.getDescription());
        event.setEventDate(eventRequest.getEventDate());
        event.setCreatedBy(creator);

        // Manteniamo il codice di condivisione esistente
        event.setShareCode(existingEvent.getShareCode());

        // Manteniamo la lista degli utenti con cui è condiviso
        event.setSharedWithUserIds(existingEvent.getSharedWithUserIds());

        Event updatedEvent = eventService.updateEvent(event);
        EventResponse eventResponse = eventService.convertEventToDto(updatedEvent);
        return ResponseEntity.ok(eventResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(new MessageResponse("Event deleted successfully."));
    }
}