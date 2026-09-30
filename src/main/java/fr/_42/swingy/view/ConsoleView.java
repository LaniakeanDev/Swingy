package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;
import fr._42.swingy.model.map.Position;

import java.util.List;
import java.util.Scanner;

/**
 * Terminal renderer for Swingy.
 *
 * <p>Uses ANSI escape codes for color. If the terminal doesn't support them
 * (e.g. some Windows consoles), the symbols still render, just without color —
 * no crash, no garbled output.</p>
 */
public class ConsoleView implements View {

    /* ------------------------------------------------------------------ */
    /*  ANSI color codes                                                   */
    /* ------------------------------------------------------------------ */
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";

    /* ------------------------------------------------------------------ */
    /*  Map glyphs                                                         */
    /* ------------------------------------------------------------------ */
    private static final char EMPTY   = '.';
    private static final char HERO    = 'H';
    private static final char VILLAIN = 'V';

    /* ------------------------------------------------------------------ */
    /*  State                                                              */
    /* ------------------------------------------------------------------ */
    private final Scanner scanner = new Scanner(System.in);

    /* ------------------------------------------------------------------ */
    /*  View implementation                                                */
    /* ------------------------------------------------------------------ */

    @Override
    public void displayMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void displayError(String error) {
        System.out.println(RED + "[!] " + error + RESET);
    }

    @Override
    public String askInput(String prompt) {
        System.out.print(CYAN + "> " + prompt + " " + RESET);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine();
    }

    @Override
    public void renderMap(GameMap map, Hero hero) {
        int size = map.getSize();
        Position heroPos = hero.getPosition();

        System.out.println();
        printHorizontalBorder(size);

        for (int y = 0; y < size; y++) {
            System.out.print("  |");
            for (int x = 0; x < size; x++) {
                Position p = new Position(x, y);

                if (heroPos != null && heroPos.equals(p)) {
                    System.out.print(BOLD + GREEN + " " + HERO + " " + RESET);
                    continue;
                }

                Object cell = map.getCell(p);
                if (cell instanceof Villain) {
                    System.out.print(RED + " " + VILLAIN + " " + RESET);
                } else if (map.isBorder(p)) {
                    System.out.print(YELLOW + " # " + RESET);
                } else {
                    System.out.print(" " + EMPTY + " ");
                }
            }
            System.out.println("|");
        }

        printHorizontalBorder(size);
        System.out.println();
    }

    @Override
    public void showHeroStats(Hero hero) {
        System.out.println(BLUE + BOLD + "╭─ " + hero.getName()
                + " the " + hero.getHeroClass().displayName() + " ─╮" + RESET);
        System.out.printf("  Level      : %d%n", hero.getLevel());
        System.out.printf("  Experience : %d (next level at %d)%n",
                hero.getExperience(), hero.experienceToNextLevel());
        System.out.printf("  Attack     : %d%n", hero.getAttack());
        System.out.printf("  Defense    : %d%n", hero.getDefense());
        System.out.printf("  Hit Points : %d / %d%n",
            hero.getCurrentHitPoints(), hero.getHitPoints());
        System.out.printf("  Artifacts  : %d%n", hero.getArtifacts().size());
        System.out.println(BLUE + "╰──────────────────────────╯" + RESET);
    }

    @Override
    public void close() {
        scanner.close();
    }

    @Override
    public void showBattleResult(EncounterResult result, Hero hero, Villain villain) {
        switch (result) {
            case HERO_WON ->
                System.out.println(GREEN + "You defeated " + villain.getName() + "!" + RESET);
            case HERO_FLED ->
                System.out.println(YELLOW + "You fled from " + villain.getName() + "." + RESET);
            case HERO_LOST ->
                System.out.println(RED + villain.getName() + " has slain you." + RESET);
        }
    }

    @Override
    public void showWinDialog(Hero hero) {
        // Console already prints the message inline; no popup.
    }

    @Override
    public void showLossDialog(Hero hero, Villain villain) {
        // Same — the controller's message is enough.
    }

    @Override
    public void displayHeroList(List<Hero> heroes) {
        if (heroes.isEmpty()) {
            System.out.println("No heroes saved. Create one to begin.");
            return;
        }
        System.out.println("Saved heroes:");
        for (int i = 0; i < heroes.size(); i++) {
            Hero h = heroes.get(i);
            System.out.printf("  %d. %s (%s, level %d)%n",
                    i + 1, h.getName(), h.getHeroClass().displayName(), h.getLevel());
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Rendering helpers                                                  */
    /* ------------------------------------------------------------------ */

    private void printHorizontalBorder(int size) {
        // One leading "  |" plus 3 chars per column plus one trailing "|"
        System.out.print("  +");
        for (int i = 0; i < size; i++) {
            System.out.print("---");
        }
        System.out.println("+");
    }
}