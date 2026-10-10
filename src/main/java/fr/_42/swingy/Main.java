package fr._42.swingy;

import java.util.Random;
import java.awt.GraphicsEnvironment;
import fr._42.swingy.controller.GameController;
import fr._42.swingy.persistence.GameRepository;
import fr._42.swingy.util.Constants;
import fr._42.swingy.validation.Validator;
import fr._42.swingy.view.View;
import fr._42.swingy.view.ConsoleView;
import fr._42.swingy.view.GuiView;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

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

        // Relaunch once with the scale flag if we weren't started with it.
        if (System.getProperty("sun.java2d.uiScale") == null
                && System.getProperty("swingy.relaunched") == null) {
            try {
                relaunchWithScale(args);
                // If relaunch succeeds, relaunchWithScale never returns (System.exit).
                // If it returns, the relaunch failed — keep going in this JVM.
            } catch (java.io.IOException | InterruptedException e) {
                System.err.println("[swingy] Could not relaunch with scale flag: " + e);
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }

        String mode = args[0].trim().toLowerCase();

        try {
            View view;
            try {
                view = createView(mode);
            } catch (IllegalArgumentException e) {
                System.err.println("[swingy] " + e.getMessage());
                printUsage();
                System.exit(Constants.EXIT_USAGE);
                return;
            }

            Validator validator = createValidator();
            GameRepository repository = new GameRepository(Constants.SAVE_FILE);
            Random random = new Random();
            new GameController(view, validator, repository, random).run();
        } catch (Exception e) {
            System.err.println("[swingy] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(Constants.EXIT_FAILURE);
        }
    }

    private static void relaunchWithScale(String[] args)
            throws java.io.IOException, InterruptedException {

        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.add("-Dsun.java2d.uiScale=2");
        cmd.add("-Dswingy.relaunched=true");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(Main.class.getName());
        for (String a : args) cmd.add(a);

        Process child = new ProcessBuilder(cmd).inheritIO().start();
        int exitCode = child.waitFor();

        // Only take over the parent's exit code if the child actually completed.
        // If the child crashed, fall through so the user sees why in this JVM.
        System.exit(exitCode);
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
                if (GraphicsEnvironment.isHeadless()) {
                    throw new IllegalArgumentException(
                        "GUI mode requires a display — no X11/Wayland available. "
                        + "Use 'console' mode, or run from a desktop session.");
                }
                return new GuiView();
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

