package fr._42.swingy;

import fr._42.swingy.controller.GameController;
import fr._42.swingy.persistence.HeroRepository;
import fr._42.swingy.util.Constants;
import fr._42.swingy.validation.Validator;
import fr._42.swingy.view.View;
import fr._42.swingy.view.ConsoleView;
// import fr._42.swingy.view.GuiView;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
// import java.util.Arrays;

/**
 * Entry point for the Swingy text-based RPG.
 *
 * Usage:
 *   java -jar swingy.jar console
 *   java -jar swingy.jar gui
 */
public final class Main {

    private Main() {
        // utility class, no instantiation
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit(Constants.EXIT_USAGE);
        }

        String mode = args[0].trim().toLowerCase();
        
        try {
            View view = createView(mode);
            Validator validator = createValidator();
            HeroRepository repository = new HeroRepository(Constants.SAVE_FILE);

            GameController controller = new GameController(view, validator, repository);
            controller.run();
        } catch (IllegalArgumentException e) {
            System.err.println("[swingy] " + e.getMessage());
            printUsage();
            System.exit(Constants.EXIT_USAGE);
        } catch (Exception e) {
            // Last-resort guard: never let a stack trace crash the jar silently
            System.err.println("[swingy] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(Constants.EXIT_FAILURE);
        }
    }

    /**
     * Factory method that turns the CLI argument into a concrete View.
     * This is the single switch point between console and GUI — everything
     * downstream only knows about the {@link View} interface.
     */
    private static View createView(String mode) {
        switch (mode) {
            case "console":
                return new ConsoleView();
            case "gui":
                // return new GuiView();
                return new ConsoleView();
            default:
                throw new IllegalArgumentException("Unknown mode: '" + mode + "'");
        }
    }

    /**
     * Bootstraps the Bean Validation entry point once, so the whole app
     * shares one Validator instance (thread-safe and expensive to build).
     */
    private static Validator createValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return new Validator(factory.getValidator());
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar swingy.jar <mode>");
        System.err.println("  mode: console | gui");
    }
}