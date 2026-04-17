package com.example.superset.service;

public class SupersetServiceException extends RuntimeException {

    public SupersetServiceException(String message) {
        super(message);
    }

    public SupersetServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
