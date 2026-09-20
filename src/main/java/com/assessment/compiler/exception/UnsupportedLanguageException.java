package com.assessment.compiler.exception;

public class UnsupportedLanguageException extends RuntimeException {

    public UnsupportedLanguageException(String language) {
        super("Lenguaje no soportado: " + language);
    }
}
