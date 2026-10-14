package fr._42.swingy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.enums.Direction;
import fr._42.swingy.model.map.GameMap;
import fr._42.swingy.model.map.Position;
import fr._42.swingy.model.battle.BattleReport;
import fr._42.swingy.model.battle.BattleSimulator;
import fr._42.swingy.persistence.GameRepository;
import fr._42.swingy.persistence.GameRepository.GameState;
import fr._42.swingy.persistence.GameRepository.GameState.Session;
import fr._42.swingy.validation.Validator;
import fr._42.swingy.view.GuiView;
import fr._42.swingy.view.View;

public class GameController {

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    private static final String NEW_HERO_CHOICE = "Create new hero";
    private static final String QUIT_CHOICE     = "Quit";
    private static final String QUIT_TO_MENU    = "Quit to Menu";

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final View view;
    private final Validator validator;
    private final GameRepository repository;
    private final Random random;
    private final BattleSimulator battleSimulator;

    private GameState persistedState;
    private List<Hero> heroList = new ArrayList<>();

    private GameMap map;
    private Hero currentHero;

    /** Signals to {@link #run()} that the game loop exited back to the menu. */
    private boolean returnToMenu = false;

    /** Why the last game session ended, if it ended. Used for menu flavour. */
    private enum EndReason { NONE, QUIT, WIN, LOSS }
    private EndReason lastEnd = EndReason.NONE;

    /** Result of attempting a move — tells the game loop how to proceed. */
    private enum MoveOutcome {
        CONTINUE,       // hero moved; keep playing
        BLOCKED,        // villain blocked the path (fled or off-map)
        HERO_DIED,      // loss dialog shown; caller should end the game
        HERO_WON        // border reached; caller should end the game
    }

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------

    public GameController(View view, Validator validator,
                          GameRepository repository, Random random) {
        this.view = view;
        this.validator = validator;
        this.repository = repository;
        this.random = random;
        this.battleSimulator = new BattleSimulator(random);
    }

    // ---------------------------------------------------------------
    // GUI routing helpers (no-ops for the console View)
    // ---------------------------------------------------------------

    private void showMenuPrimaryActions(List<String> labels) {
        if (view instanceof GuiView gv) gv.setMenuPrimaryActions(labels);
    }

    private void showMenuChoices(List<String> labels, List<Hero> heroList) {
        if (view instanceof GuiView gv) gv.setMenuChoices(labels);
        else view.displayHeroList(heroList);
    }

    private void showGamePrimaryActions(List<String> labels) {
        if (view instanceof GuiView gv) gv.setGamePrimaryActions(labels);
    }

    private void showGameChoices(List<String> labels) {
        if (view instanceof GuiView gv) gv.setGameChoices(labels);
    }

    private void switchToMenu() {
        if (view instanceof GuiView gv) gv.showMenu();
    }

    private void switchToGame() {
        if (view instanceof GuiView gv) gv.showGame();
    }

    /**
     * Ask the view for input, but sanitise the queue first so that any
     * button click that arrived between prompts (i.e. a click on the
     * previous screen's buttons before the new buttons rendered) doesn't
     * leak into this prompt.
     */
    private String prompt(String message) {
        if (view instanceof GuiView gv) {
            gv.drainInput();
        }
        return view.askInput(message).trim();
    }

    // ---------------------------------------------------------------
    // Entry point — outer loop so "Quit to Menu" really returns to menu
    // ---------------------------------------------------------------

    public void run() {
        persistedState = repository.load();
        heroList = new ArrayList<>(persistedState.roster);

        try {
            boolean appRunning = true;

            while (appRunning) {
                // --- Menu phase ---------------------------------------------
                switchToMenu();
                currentHero = mainMenu();

                if (currentHero == null) {           // user clicked "Quit"
                    view.displayMessage("Goodbye.");
                    appRunning = false;
                    continue;
                }

                // --- Session setup -----------------------------------------
                prepareSession();

                // --- Play phase --------------------------------------------
                switchToGame();
                returnToMenu = false;
                lastEnd = EndReason.NONE;

                gameLoop();

                if (returnToMenu) {
                    view.displayMessage("Returning to main menu.");
                    // Session already flushed inside gameLoop's exit path.
                    map = null;
                    currentHero = null;
                    continue;
                }

                // Game ended via win/loss without returnToMenu — shouldn't
                // happen because gameLoop sets returnToMenu on those too.
                appRunning = false;
            }

        } catch (Exception e) {
            view.displayError("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Only flush the roster here; per-session saves happen inline.
            if (heroList != null && !heroList.isEmpty()) {
                saveRosterOnly();
                System.out.println("Your hero(es) have been saved on exit.");
            }
            view.close();
        }
    }

    /** Builds/restores the map for the currently selected hero. */
    private void prepareSession() {
        int expectedSize = mapSizeFor(currentHero.getLevel());
        Session saved    = persistedState.sessions.get(currentHero.getName());
        boolean canResume = saved != null && saved.mapSize == expectedSize;

        map = new GameMap(expectedSize, random);
        if (currentHero.getPosition() == null) {
            currentHero.setPosition(map.center());
        }
        map.placeHero(currentHero, currentHero.getPosition());

        if (canResume) {
            map.replaceVillains(saved.villains);
            view.displayMessage("Resuming saved session.");
        } else {
            map.generateVillains(villainCountFor(expectedSize),
                                 currentHero.getLevel());
        }
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    private void saveRosterOnly() {
        persistedState.roster = heroList;
        if (persistedState.sessions == null) {
            persistedState.sessions = new java.util.LinkedHashMap<>();
        }
        repository.save(persistedState);
        // view.displayMessage("Your hero(es) have been saved.");
    }

    private void saveAndExit() {
        if (map == null) {
            if (heroList != null && currentHero != null) {
                saveRosterOnly();
            }
            return;
        }

        persistedState.roster = heroList;
        if (persistedState.sessions == null) {
            persistedState.sessions = new java.util.LinkedHashMap<>();
        }
        // Merge: keep every other hero's session, replace only the current one.
        persistedState.sessions.put(
                currentHero.getName(),
                new Session(map.getSize(), map.getVillains()));

        repository.save(persistedState);
        view.displayMessage("Your game has been saved.");
    }

    private int villainCountFor(int mapSize) {
        return Math.max(2, mapSize / 3);
    }

    private int mapSizeFor(int level) {
        return (level - 1) * 5 + 10 - (level % 2);
    }

    // ---------------------------------------------------------------
    // Game loop
    // ---------------------------------------------------------------

    private void gameLoop() {
        showGamePrimaryActions(List.of(QUIT_TO_MENU));
        List<String> moveChoices = List.of("North", "East", "South", "West");

        boolean running = true;
        while (running) {
            view.renderMap(map, currentHero);
            view.showHeroStats(currentHero);
            showGameChoices(moveChoices);
            String input = prompt("Choose an action (north, east, south, west or \"quit to menu\")");

            if (QUIT_TO_MENU.equalsIgnoreCase(input)) {
                view.displayMessage("Saving and returning to menu…");
                saveAndExit();
                lastEnd = EndReason.QUIT;
                returnToMenu = true;
                running = false;
                continue;
            }

            Direction dir = Direction.fromString(input.toLowerCase());
            if (dir == null) {
                view.displayError("Invalid direction: '" + input + "'");
                continue;
            }

            MoveOutcome outcome = handleMove(dir);
            switch (outcome) {
                case HERO_WON -> {
                    lastEnd = EndReason.WIN;
                    returnToMenu = true;
                    running = false;
                }
                case HERO_DIED -> {
                    lastEnd = EndReason.LOSS;
                    returnToMenu = true;
                    running = false;
                }
                case CONTINUE, BLOCKED -> { /* keep playing */ }
            }
        }
    }

    // ---------------------------------------------------------------
    // Movement
    // ---------------------------------------------------------------

    /**
     * Attempt to move the hero one tile in {@code dir}.
     * Returns how the game loop should proceed. Never calls System.exit —
     * the caller decides what "game over" means (return to menu, quit, etc.).
     */
    private MoveOutcome handleMove(Direction dir) {
        Position current = currentHero.getPosition();
        Position next    = map.getNextPosition(current, dir);

        if (!map.isInside(next)) {
            view.displayError("You can't leave the map.");
            return MoveOutcome.BLOCKED;
        }

        Villain villain = map.getVillainAt(next);
        if (villain != null) {
            MoveOutcome combat = resolveEncounter(currentHero, villain, next);
            if (combat != MoveOutcome.CONTINUE) {
                return combat;   // fled, died, or otherwise did not move
            }
        } else {
            // Plain move — no opposition.
            currentHero.setPosition(next);
        }

        // Win check runs after the hero is standing on the target tile.
        if (map.isBorder(next)) {
            view.displayMessage("You reached the border — you win!");
            view.showWinDialog(currentHero);
            saveAndExit();
            return MoveOutcome.HERO_WON;
        }

        return MoveOutcome.CONTINUE;
    }

    /**
     * Fight/run the villain standing on {@code target}. On a win, moves the
     * hero onto the tile and removes the villain. Returns:
     *   CONTINUE   — hero won and moved onto the tile
     *   BLOCKED    — hero fled, tile still occupied
     *   HERO_DIED  — hero lost (dialog shown, session saved)
     */
    private MoveOutcome resolveEncounter(Hero hero, Villain villain, Position target) {
        EncounterResult result = handleEncounter(hero, villain);
        switch (result) {
            case HERO_WON -> {
                view.displayMessage("You defeated " + villain.getName() + "!");
                map.removeVillain(villain);
                hero.setPosition(target);
                return MoveOutcome.CONTINUE;
            }
            case HERO_FLED -> {
                view.displayMessage("You fled. The villain still blocks the path.");
                return MoveOutcome.BLOCKED;
            }
            case HERO_LOST -> {
                view.displayMessage("You were defeated. Game over.");
                view.showLossDialog(hero, villain);
                saveAndExit();
                return MoveOutcome.HERO_DIED;
            }
            default -> {
                return MoveOutcome.CONTINUE;
            }
        }
    }

    // ---------------------------------------------------------------
    // Main menu — "Create" / "Quit" on primary row, heroes on choice row
    // ---------------------------------------------------------------

    private Hero mainMenu() {
        announceMenuContext();
        view.displayMessage("Welcome to Swingy!");

        while (true) {
            // Primary row — never mixed with the hero buttons.
            showMenuPrimaryActions(List.of(NEW_HERO_CHOICE, QUIT_CHOICE));

            // Choice row — one button per saved hero.
            List<String> heroLabels = new ArrayList<>();
            for (Hero h : heroList) {
                heroLabels.add(h.getName() + "  (Lvl " + h.getLevel() + ")");
            }
            showMenuChoices(heroLabels, heroList);

            String choice = prompt("Choose a hero (or \"" + NEW_HERO_CHOICE + "\" or \"quit\")");

            if (NEW_HERO_CHOICE.equalsIgnoreCase(choice)) {
                Hero created = createNewHero();
                if (created != null) return created;
                continue;   // creation cancelled or failed → show menu again
            }

            if (QUIT_CHOICE.equalsIgnoreCase(choice)) {
                return null;
            }

            // Match by prefix (the label is "Name  (Lvl N)").
            for (Hero h : heroList) {
                if (choice.startsWith(h.getName())) {
                    return h;
                }
            }
            view.displayError("Unknown hero: '" + choice + "'");
        }
    }

    /** Greets the player based on the previous session's ending. */
    private void announceMenuContext() {
        switch (lastEnd) {
            case WIN  -> view.displayMessage("Welcome back, champion.");
            case LOSS -> view.displayMessage("That was rough. Try again?");
            case QUIT -> view.displayMessage("Session saved.");
            case NONE -> { /* first run, no greeting needed */ }
        }
        lastEnd = EndReason.NONE;
    }

    // ---------------------------------------------------------------
    // Hero creation
    // ---------------------------------------------------------------

    private HeroClass askHeroClass() {
        List<String> labels = new ArrayList<>();
        for (HeroClass hc : HeroClass.values()) {
            labels.add(hc.name());
        }

        while (true) {
            showMenuPrimaryActions(labels);
            String choice = prompt("Choose a hero class\n(available classes: " + HeroClass.values()[0].displayList() + ")\n");

            HeroClass heroClass = HeroClass.fromString(choice);
            if (heroClass != null) {
                return heroClass;
            }
            view.displayError("Unknown hero class: '" + choice + "'");
        }
    }

    private boolean checkNameUnicity(String name) {
        for (Hero h : heroList) {
            if (h.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }

    private Hero createNewHero() {
        String name;
        while (true) {
            name = view.askText("Enter hero name");
            if (name == null) {
                view.displayMessage("Creation cancelled.");
                return null;
            }
            name = name.trim();
            if (name.isEmpty()) {
                view.displayError("Name cannot be empty.");
                continue;
            }
            if (!checkNameUnicity(name)) {
                view.displayError("Hero name already taken.");
                continue;
            }
            break;
        }

        HeroClass heroClass = askHeroClass();
        Hero newHero = new Hero.HeroBuilder()
                .name(name)
                .heroClass(heroClass)
                .build();

        if (!validator.isValid(newHero)) {
            view.displayError(validator.validateAndCollect(newHero));
            return null;
        }

        heroList.add(newHero);
        return newHero;
    }

    // ---------------------------------------------------------------
    // Encounters — Fight / Run, then Keep / Discard (choice row)
    // ---------------------------------------------------------------

    private EncounterResult handleEncounter(Hero hero, Villain villain) {
        view.displayMessage(String.format(
                "A %s (power %d) blocks your path!",
                villain.getName(), villain.getAttack()));

        showGameChoices(List.of("Fight", "Run"));
        String choice = prompt("Fight or Run?").toLowerCase();
        if (choice.startsWith("r")) {
            if (random.nextBoolean()) {
                return EncounterResult.HERO_FLED;
            }
            view.displayMessage("You failed to escape — you must fight!");
        }

        BattleReport report = battleSimulator.fight(hero, villain);
        report.log().forEach(view::displayMessage);
        report.drop().ifPresent(artifact -> promptKeepArtifact(hero, artifact));

        return report.result();
    }

    /**
     * Ask the player whether to keep the dropped artifact.
     *
     * The queue is drained in {@link #prompt(String)}, so stale clicks from
     * the Fight/Run row cannot leak into this prompt. Choices are matched
     * defensively so unexpected input is logged rather than silently
     * treated as "discard".
     */
    private void promptKeepArtifact(Hero hero, Artifact artifact) {
        showGameChoices(List.of("Keep", "Discard"));
        String answer = prompt("Keep " + artifact.getName() + "?").toLowerCase();

        boolean wantsKeep    = answer.startsWith("k") || answer.startsWith("y");
        boolean wantsDiscard = answer.startsWith("d") || answer.startsWith("n");

        if (!wantsKeep && !wantsDiscard) {
            view.displayError("Unrecognised choice '" + answer
                    + "' — leaving " + artifact.getName() + " behind.");
            return;
        }
        if (wantsDiscard) {
            view.displayMessage("You leave it behind.");
            return;
        }

        if (hero.equipArtifact(artifact)) {
            view.displayMessage("Equipped " + artifact.getName() + ".");
        } else {
            view.displayMessage("Your class can't use that artifact. Discarded.");
        }
    }
}