package com.scalaris.auth.web;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.scalaris.api.ApiError;
import com.scalaris.auth.repo.UserRepository;
import com.scalaris.auth.service.AuthService;
import com.scalaris.auth.service.TokenService;
import com.scalaris.auth.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth - Login", description = "Inicio de sesión, refresh y logout.")
@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final AuthService auth;
    private final TokenService tokens;
    private final UserRepository users;

    public LoginController(AuthService auth, TokenService tokens, UserRepository users) {
        this.auth = auth;
        this.tokens = tokens;
        this.users = users;
    }

    @Operation(summary = "Iniciar sesión",
            description = "Valida credenciales y devuelve access/refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación OK",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de request fallida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = """
                                    { "email": "user@demo.com", "password": "Password1" }
                                    """)
                    )
            )
            @RequestBody @Valid LoginRequest req
    ) {
        var user = auth.authenticate(req);
        var issued = tokens.issueTokens(user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                issued.accessToken(),
                issued.accessExpiresInSeconds(),
                issued.refreshToken(),
                issued.refreshExpiresInSeconds()
        ));
    }

    @Operation(summary = "Refresh token",
            description = "Valida refreshToken y emite nuevos tokens. Recomendado rotación (refresh nuevo).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh OK",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de request fallida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Refresh inválido / vencido / revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(value = """
                                    { "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
                                    """)
                    )
            )
            @RequestBody @Valid RefreshRequest req
    ) {
        var decoded = tokens.verify(req.refreshToken());
        UUID userId = UUID.fromString(decoded.getSubject());

        var user = users.findById(userId)
                .orElseThrow(() -> new JWTVerificationException("Usuario inexistente"));

        // Nota: TokenService.refresh(...) hoy vuelve a verificar el JWT internamente.
        // Si querés evitar doble verify, abajo te dejo un patch opcional.
        var issued = tokens.refresh(decoded, user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                issued.accessToken(),
                issued.accessExpiresInSeconds(),
                issued.refreshToken(),
                issued.refreshExpiresInSeconds()
        ));
    }

    @Operation(summary = "Cerrar sesión (logout)",
            description = "Revoca el refresh token provisto. El access token (si existe) expira por TTL.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout OK (refresh revocado)"),
            @ApiResponse(responseCode = "400", description = "Validación de request fallida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Refresh inválido / ya revocado / vencido",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(value = """
                                    { "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
                                    """)
                    )
            )
            @RequestBody @Valid RefreshRequest req
    ) {
        tokens.revokeRefresh(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
