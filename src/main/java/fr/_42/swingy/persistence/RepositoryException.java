package fr._42.swingy.persistence;

/** Unchecked wrapper so the controller isn't forced to catch IOException everywhere. */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    public RepositoryException(String message) {
        super(message);
    }
}
