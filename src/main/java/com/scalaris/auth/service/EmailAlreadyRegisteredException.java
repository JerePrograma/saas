package com.scalaris.auth.service;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("Ya te encuentras registrado");
    }
}
