package fr._42.swingy.persistence;

/** Unchecked wrapper so the controller isn't forced to catch IOException everywhere. */
/**
 * Unchecked = extends RuntimeException (not Exception). The Java compiler does not force callers to declare throws or wrap it in try/catch. Only checked exceptions (subclasses of Exception that aren't RuntimeException) do that.

* Wrapper = it exists to wrap another exception (the cause passed to super(message, cause)), typically a checked one like IOException, and re-throw it as unchecked.

* The persistence layer (repositories) almost certainly does file I/O — reading/writing save files for Swingy. Those operations throw IOException, which is checked.

* Propagating throws IOException up the call stack means the controller (and possibly the view) would have to declare or handle IOException too — even though a controller has no meaningful way to recover from "disk write failed."

* Wrapping it in RepositoryException lets the repository throw freely and the controller stay clean, only handling exceptions if it actually cares

* Keeps method signatures clean and focused. (no public void save(Hero h) throws IOException { ... })

* Avoids "exception pollution" — checked exceptions bubbling up through layers that can't act on them.

* The original cause is preserved via cause, so you can still log/debug the real IOException.

* Unchecked exceptions can be silently ignored — nothing forces the caller to think about failure.
 */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    public RepositoryException(String message) {
        super(message);
    }
}
