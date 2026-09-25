package fr._42.swingy.validation;

import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin façade over Bean Validation so the rest of the code never
 * touches javax.validation directly. Makes swapping libraries trivial.
 */
public class Validator {

    private final javax.validation.Validator delegate;

    public Validator(javax.validation.Validator delegate) {
        this.delegate = delegate;
    }

    public <T> boolean isValid(T object, Class<?>... groups) {
        return delegate.validate(object, groups).isEmpty();
    }

    public <T> String validateAndCollect(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = delegate.validate(object, groups);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("\n"));
    }
}