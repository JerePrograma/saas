package com.scalaris.auth.web;

import com.scalaris.api.ApiError;
import com.scalaris.auth.service.RegistrationService;
import com.scalaris.auth.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth - Register", description = "Registro de usuario (check email + alta).")
@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final RegistrationService registration;

    public RegisterController(RegistrationService registration) {
        this.registration = registration;
    }

    @Operation(summary = "Chequear disponibilidad de email",
            description = "Helper para el Step1 (React). Si está ocupado retorna 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email disponible"),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    @PostMapping("/register/check-email")
    public ResponseEntity<Void> checkEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CheckEmailRequest.class),
                            examples = @ExampleObject(value = """
                                    { "email": "user@demo.com" }
                                    """)
                    )
            )
            @RequestBody @Valid CheckEmailRequest req
    ) {
        boolean available = registration.isEmailAvailable(req.email().trim());
        return available ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @Operation(summary = "Registrar usuario",
            description = "Registro final (Step1 + Step2). Si el email ya existe retorna 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación / reglas de negocio fallidas",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "fullName": "Juan Pérez",
                                      "email": "juan@demo.com",
                                      "password": "Abcdef12",
                                      "confirmPassword": "Abcdef12",
                                      "acceptedTerms": true,
                                      "taxPosition": "MONOTRIBUTO",
                                      "companyStructure": "UNIPERSONAL"
                                    }
                                    """)
                    )
            )
            @RequestBody @Valid RegisterRequest req
    ) {
        var user = registration.register(req);

        var body = new RegisterResponse(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Schema(name = "CheckEmailRequest")
    public record CheckEmailRequest(
            @NotBlank @Email @Schema(example = "user@demo.com") String email
    ) {}

    @Schema(name = "RegisterResponse")
    public record RegisterResponse(
            @Schema(example = "b3b1c2c0-9c7a-4a3c-9f14-9d2f5e5b5a01") String id,
            @Schema(example = "Juan Pérez") String fullName,
            @Schema(example = "juan@demo.com") String email,
            @Schema(example = "EMPLOYEE") String role,
            @Schema(example = "2026-01-13T16:12:30.309-03:00") String createdAt
    ) {}
}
