package fr._42.swingy.validation;

import javax.validation.ConstraintViolation;
// import javax.validation.ValidatorFactory;

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

    public boolean isValidHeroName(String name) {
        // Example validation: name must be between 3 and 20 characters and only contain letters and spaces
        return name != null && name.matches("[A-Za-z ]{3,20}");
    }

    public boolean isValidHeroClass(String className) {
        // Example validation: hero class must be one of the predefined classes
        return className != null && (className.equalsIgnoreCase("CULTURE_CITIZEN") ||
                className.equalsIgnoreCase("CONTACT_AGENT") ||
                className.equalsIgnoreCase("CULTURE_DRONE_CIVILIAN") ||
                className.equalsIgnoreCase("CULTURE_DRONE_CONTACT") ||
                className.equalsIgnoreCase("CONTRACTOR_AGENT") ||
                className.equalsIgnoreCase("CULTURE_REFERER") ||
                className.equalsIgnoreCase("SPECIAL_CIRCUMSTANCES_AGENT"));
    }
}