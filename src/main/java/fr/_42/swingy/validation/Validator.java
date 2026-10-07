package fr._42.swingy.validation;

import javax.validation.ConstraintViolation;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Validator {

    private final javax.validation.Validator delegate;

    public Validator(javax.validation.Validator delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
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