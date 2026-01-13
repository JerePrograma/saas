package com.scalaris.auth.service;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Correo o contraseña inválidos. Intenta nuevamente.");
    }
}
