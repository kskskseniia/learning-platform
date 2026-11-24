package com.example.learningplatform.controller;

import com.example.learningplatform.dto.ProfileRequest;
import com.example.learningplatform.dto.ProfileResponse;
import com.example.learningplatform.dto.ProfileUpdateRequest;
import com.example.learningplatform.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profiles", description = "Управление профилями пользователей")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Получить профили", description = "Возвращает все профили пользователей")
    @ApiResponse(responseCode = "200", description = "Профили получены")
    public List<ProfileResponse> findAll() {
        return profileService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить профиль", description = "Возвращает профиль по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Профиль найден"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден")
    })
    public ProfileResponse findById(@PathVariable Integer id) {
        return profileService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать профиль", description = "Создаёт единственный профиль пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Профиль создан"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "409", description = "У пользователя уже есть профиль")
    })
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.create(request);
        return ResponseEntity.created(URI.create("/api/profiles/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить профиль", description = "Обновляет данные профиля без смены пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден")
    })
    public ProfileResponse update(@PathVariable Integer id, @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить профиль", description = "Удаляет профиль по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Профиль удалён"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
