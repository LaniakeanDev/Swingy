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

    private final View view;
    private final Validator validator;
    private final GameRepository repository;
    private GameMap map;
    private Hero currentHero;
    private List<Hero> heroList = new ArrayList<>();
    private final Random random;
    private final BattleSimulator battleSimulator;
    private GameState persistedState;

    // Marker returned by the hero-selection dialog for "create new".
    private static final String NEW_HERO_CHOICE = "+ Create new hero";

    public GameController(View view, Validator validator,
                          GameRepository repository, Random random) {
        this.view = view;
        this.validator = validator;
        this.repository = repository;
        this.random = random;
        this.battleSimulator = new BattleSimulator(random);
    }

    // ---------------------------------------------------------------
    // Small helper: route setActions/setHeroChoices only when GUI
    // ---------------------------------------------------------------

    private void showActions(List<String> labels) {
        if (view instanceof GuiView gv) {
            gv.setActions(labels);
        }
        // Console view ignores this and waits on askInput as before.
    }

    private void showHeroChoices(List<String> labels) {
        if (view instanceof GuiView gv) {
            gv.setHeroChoices(labels);
        }
    }

    // ---------------------------------------------------------------

    public void run() {
        persistedState = repository.load();
        heroList = new ArrayList<>(persistedState.roster);

        try {
            while ((currentHero = mainMenu()) == null) {
                view.displayMessage("No hero selected. Let's try again.");
            }

            int expectedSize = mapSizeFor(currentHero.getLevel());

            Session saved = persistedState.sessions.get(currentHero.getName());
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

            gameLoop();
        } catch (Exception e) {
            view.displayError("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (map != null && currentHero != null) {
                saveAndExit();
            } else if (heroList != null && !heroList.isEmpty()) {
                saveRosterOnly();
            }
            view.close();
        }
    }

    private void saveRosterOnly() {
        persistedState.roster = heroList;
        if (persistedState.sessions == null) {
            persistedState.sessions = new java.util.LinkedHashMap<>();
        }
        repository.save(persistedState);
        view.displayMessage("Your hero(es) have been saved.");
    }

    private void saveAndExit() {
        if (map == null) {
            if (heroList != null && currentHero != null) {
                saveRosterOnly();
            }
            return;
        }

        persistedState.roster = heroList;
        // Ensure sessions map exists (it may be null if load() built it oddly).
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

    // ---------------------------------------------------------------
    // Game loop — buttons for moves
    // ---------------------------------------------------------------

    private void gameLoop() {
        List<String> moves = List.of("North", "East", "South", "West", "Exit");

        boolean running = true;
        while (running) {
            view.renderMap(map, currentHero);
            view.showHeroStats(currentHero);

            showActions(moves);
            String input = view.askInput("Choose an action").trim();

            if ("exit".equalsIgnoreCase(input)) {
                running = false;
                continue;
            }

            Direction dir = Direction.fromString(input.toLowerCase());
            if (dir == null) {
                view.displayError("Invalid direction: '" + input + "'");
                continue;
            }

            handleMove(dir);
        }
    }

    private int mapSizeFor(int level) {
        return (level - 1) * 5 + 10 - (level % 2);
    }

    private void handleMove(Direction dir) {
        Position current = currentHero.getPosition();
        Position next    = map.getNextPosition(current, dir);

        if (!map.isInside(next)) {
            view.displayError("You can't leave the map.");
            return;
        }

        Villain villain = map.getVillainAt(next);
        if (villain != null) {
            EncounterResult result = handleEncounter(currentHero, villain);
            switch (result) {
                case HERO_WON -> {
                    view.displayMessage("You defeated " + villain.getName() + "!");
                    map.removeVillain(villain);
                    currentHero.setPosition(next);
                }
                case HERO_FLED -> {
                    view.displayMessage("You fled. The villain still blocks the path.");
                    return;
                }
                case HERO_LOST -> {
                    view.displayMessage("You were defeated. Game over.");
                    view.showLossDialog(currentHero, villain);
                    saveAndExit();
                    System.exit(0);
                    return;
                }
            }
        } else {
            currentHero.setPosition(next);
        }

        if (map.isBorder(next)) {
            view.displayMessage("You reached the border — you win!");
            view.showWinDialog(currentHero);
            saveAndExit();
            System.exit(0);
        }
    }

    // ---------------------------------------------------------------
    // Main menu — click a hero, or click "+ Create new hero"
    // ---------------------------------------------------------------

    private Hero mainMenu() {
        view.displayMessage("Welcome to Swingy!");

        while (true) {
            List<String> labels = new ArrayList<>();
            for (Hero h : heroList) {
                labels.add(h.getName() + "  (Lvl " + h.getLevel() + ")");
            }
            labels.add(NEW_HERO_CHOICE);   // always the last option

            showHeroChoices(labels);
            String choice = view.askInput("Choose a hero").trim();

            if (NEW_HERO_CHOICE.equalsIgnoreCase(choice)) {
                Hero created = createNewHero();
                if (created != null) return created;
                continue;   // creation cancelled or failed → show menu again
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

    // ---------------------------------------------------------------
    // Hero creation — class picked from buttons, name via askText
    // ---------------------------------------------------------------

    private HeroClass askHeroClass() {
        List<String> labels = new ArrayList<>();
        for (HeroClass hc : HeroClass.values()) {
            labels.add(hc.name());
        }

        while (true) {
            showActions(labels);
            String choice = view.askInput("Choose a hero class").trim();

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
    // Encounters — Fight / Run buttons, then Keep / Discard buttons
    // ---------------------------------------------------------------

    private EncounterResult handleEncounter(Hero hero, Villain villain) {
        view.displayMessage(String.format(
                "A %s (power %d) blocks your path!",
                villain.getName(), villain.getAttack()));

        showActions(List.of("Fight", "Run"));
        String choice = view.askInput("Fight or Run?").trim().toLowerCase();
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

    private void promptKeepArtifact(Hero hero, Artifact artifact) {
        showActions(List.of("Keep", "Discard"));
        String keep = view.askInput(
                "Keep " + artifact.getName() + "?").trim().toLowerCase();

        if (!keep.startsWith("k") && !keep.startsWith("y")) {
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










// package fr._42.swingy.controller;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;
// import java.util.Random;

// import fr._42.swingy.model.entity.Artifact;
// import fr._42.swingy.model.entity.Hero;
// import fr._42.swingy.model.entity.Villain;
// import fr._42.swingy.model.enums.EncounterResult;
// import fr._42.swingy.model.enums.HeroClass;
// import fr._42.swingy.model.enums.Direction;
// import fr._42.swingy.model.map.GameMap;
// import fr._42.swingy.model.map.Position;
// import fr._42.swingy.model.battle.BattleReport;
// import fr._42.swingy.model.battle.BattleSimulator;
// import fr._42.swingy.persistence.GameRepository;
// import fr._42.swingy.persistence.GameRepository.GameState;
// import fr._42.swingy.persistence.GameRepository.GameState.Session;
// import fr._42.swingy.validation.Validator;
// import fr._42.swingy.view.View;

// public class GameController {

//     private final View view;
//     private final Validator validator;
//     private final GameRepository repository;
//     private GameMap map;
//     private Hero currentHero;
//     private List<Hero> heroList = new ArrayList<>();
//     private final Random random;
//     private final BattleSimulator battleSimulator;

//     public GameController(View view, Validator validator, GameRepository repository, Random random) {
//         this.view = view;
//         this.validator = validator;
//         this.repository = repository;
//         this.random = random;
//         this.battleSimulator = new BattleSimulator(random);
//     }

//     public void run() {
//         GameState state = repository.load();
//         heroList = new ArrayList<>(state.roster);

//         try {
//             while ((currentHero = mainMenu()) == null) {
//                 view.displayMessage("No hero selected. Let's try again.");
//             }

//             int expectedSize = mapSizeFor(currentHero.getLevel());

//             // Resume only if this hero has a saved session AND the map size
//             // on disk still matches what this hero's level implies.
//             Session saved = state.sessions.get(currentHero.getName());
//             boolean canResume = saved != null
//                     && saved.mapSize == expectedSize;

//             map = new GameMap(expectedSize, random);
//             if (currentHero.getPosition() == null) {
//                 currentHero.setPosition(map.center());
//             }
//             map.placeHero(currentHero, currentHero.getPosition());

//             if (canResume) {
//                 map.replaceVillains(saved.villains);
//                 view.displayMessage("Resuming saved session.");
//             } else {
//                 map.generateVillains(villainCountFor(expectedSize), currentHero.getLevel());
//             }

//             gameLoop();
//         } catch (Exception e) {
//             view.displayError("Fatal error: " + e.getMessage());
//             e.printStackTrace();
//         } finally {
//             if (map != null && currentHero != null) {
//                 saveAndExit();
//             } else if (heroList != null && !heroList.isEmpty()) {
//                 saveRosterOnly();
//             }
//             view.close();
//         }
//     }

//     private void saveRosterOnly() {
//         GameState state = new GameState();
//         state.roster = heroList;
//         state.sessions = new java.util.LinkedHashMap<>();
//         repository.save(state);
//         view.displayMessage("Your hero(es) have been saved.");
//     }

//     private void saveAndExit() {
//         if (map == null) {
//             // Nothing to save — we never got far enough to build a map.
//             // Still flush the roster so any hero created during mainMenu survives.
//             if (heroList != null && currentHero != null) {
//                 GameState state = new GameState();
//                 state.roster = heroList;
//                 state.sessions = new java.util.LinkedHashMap<>();
//                 repository.save(state);
//                 view.displayMessage("Your hero(es) have been saved.");
//             }
//             return;
//         }

//         GameState state = new GameState();
//         state.roster = heroList;
//         state.sessions = new java.util.LinkedHashMap<>();
//         state.sessions.put(
//                 currentHero.getName(),
//                 new Session(map.getSize(), map.getVillains()));

//         repository.save(state);
//         view.displayMessage("Your game has been saved.");
//     }

//     private int villainCountFor(int mapSize) {
//         return Math.max(2, mapSize / 3);
//     }

//     private void gameLoop() {
//         boolean running = true;
//         while (running) {
//             view.renderMap(map, currentHero);
//             view.showHeroStats(currentHero);

//             String input = view.askInput("Move (north/east/south/west) or 'exit': ").trim().toLowerCase();

//             if ("exit".equals(input)) {
//                 running = false;
//                 continue;
//             }

//             Direction dir = Direction.fromString(input);   // returns null if invalid
//             if (dir == null) {
//                 view.displayError("Invalid direction: '" + input + "'");
//                 continue;
//             }

//             handleMove(dir);

//             // if (currentHero.getPosition() != null && map.isBorder(currentHero.getPosition())) {
//             //     view.displayMessage("You reached the border — you win!");
//             //     running = false;
//             // }
//         }
//     }

//     private int mapSizeFor(int level) {
//         return (level - 1) * 5 + 10 - (level % 2);
//     }

//     private void handleMove(Direction dir) {
//         Position current = currentHero.getPosition();
//         Position next    = map.getNextPosition(current, dir);

//         if (!map.isInside(next)) {
//             view.displayError("You can't leave the map.");
//             return;
//         }

//         // 1. Villain encounter — fight/run happens BEFORE the border win check,
//         //    because a villain standing on the border must be cleared first.
//         Villain villain = map.getVillainAt(next);
//         if (villain != null) {
//             EncounterResult result = handleEncounter(currentHero, villain);
//             switch (result) {
//                 case HERO_WON -> {
//                     view.displayMessage("You defeated " + villain.getName() + "!");
//                     map.removeVillain(villain);
//                     currentHero.setPosition(next);
//                     // Fall through to check if this winning move also reached the border.
//                 }
//                 case HERO_FLED -> {
//                     view.displayMessage("You fled. The villain still blocks the path.");
//                     return;
//                 }
//                 case HERO_LOST -> {
//                     view.displayMessage("You were defeated. Game over.");
//                     view.showLossDialog(currentHero, villain);
//                     saveAndExit();
//                     System.exit(0);
//                     return;
//                 }
//             }
//         } else {
//             // No villain — move directly onto the tile.
//             currentHero.setPosition(next);
//         }

//         // 2. Win check runs AFTER the hero is on the tile, whether it was
//         //    empty or occupied by a defeated villain.
//         if (map.isBorder(next)) {
//             view.displayMessage("You reached the border — you win!");
//             view.showWinDialog(currentHero);
//             saveAndExit();
//             System.exit(0);
//         }
//     }

//     private Hero mainMenu() {
//         view.displayMessage("Welcome to Swingy!");

//         while (true) {
//             String input;
//             if (!heroList.isEmpty()) {
//                 view.displayMessage("Available heroes:");
//                 for (int i = 0; i < heroList.size(); i++) {
//                     Hero h = heroList.get(i);
//                     view.displayMessage("  " + (i + 1) + ". "
//                             + h.getName() + " (Level " + h.getLevel() + ")");
//                 }
//                 input = view.askInput("Select a hero by number, or 0 to create a new one: ").trim();
//             }
//             else {
//                 view.displayMessage("Please create a new hero: ");
//                 return createNewHero();
//             }

//             int choice;
//             try {
//                 choice = Integer.parseInt(input);
//             } catch (NumberFormatException e) {
//                 view.displayError("Please enter a number.");
//                 continue;
//             }

//             if (choice == 0) {
//                 return createNewHero();
//             }
//             if (choice < 1 || choice > heroList.size()) {
//                 view.displayError("No hero with number " + choice + ".");
//                 continue;
//             }
//             return heroList.get(choice - 1);
//         }
//     }

//     private String askHeroClass() {
//         while (true) {
//             view.displayMessage("Available hero classes:");
//             for (HeroClass hc : HeroClass.values()) {
//                 view.displayMessage("  - " + hc.name());
//             }
//             String className = view.askInput("Enter hero class: ").trim();
//             HeroClass heroClass = HeroClass.fromString(className);
//             if (heroClass != null) {
//                 return className;
//             }
//             view.displayError("Unknown hero class: '" + className + "'");
//         }
//     }

//     private boolean checkNameUnicity(String name) {
//         for (Hero h : heroList) {
//             if (h.getName().equalsIgnoreCase(name)) {
//                 return false;
//             }
//         }
//         return true;
//     }

//     private Hero createNewHero() {
//         while (true) {
//             String name = view.askInput("Enter hero name: ").trim();
//             while (!checkNameUnicity(name)) {
//                 view.displayMessage("Hero name already taken. Please choose another.");
//                 name = view.askInput("Enter hero name: ").trim();
//             }

//             HeroClass heroClass = HeroClass.fromString(askHeroClass());
//             Hero newHero = new Hero.HeroBuilder()
//                     .name(name)
//                     .heroClass(heroClass)
//                     .build();

//             if (!validator.isValid(newHero)) {
//                 view.displayError(validator.validateAndCollect(newHero));
//                 continue;
//             }

//             heroList.add(newHero);
//             return newHero;
//         }
//     }

//     private EncounterResult handleEncounter(Hero hero, Villain villain) {
//         view.displayMessage(String.format(
//             "A %s (power %d) blocks your path!", villain.getName(), villain.getAttack()));

//         String choice = view.askInput("Fight or Run? [f/r]: ").trim().toLowerCase();
//         if (choice.startsWith("r")) {
//             if (random.nextBoolean()) {              // ← still uses `random`
//                 return EncounterResult.HERO_FLED;
//             }
//             view.displayMessage("You failed to escape — you must fight!");
//         }

//         BattleReport report = battleSimulator.fight(hero, villain);

//         // Narrate every exchange
//         report.log().forEach(view::displayMessage);

//         // Handle the drop, if any
//         report.drop().ifPresent(artifact -> promptKeepArtifact(hero, artifact));

//         return report.result();
//     }

//     private void promptKeepArtifact(Hero hero, Artifact artifact) {
//         String keep = view.askInput("Keep it? [y/n]: ").trim().toLowerCase();
//         if (!keep.startsWith("y")) {
//             view.displayMessage("You leave it behind.");
//             return;
//         }
//         if (hero.equipArtifact(artifact)) {
//             view.displayMessage("Equipped " + artifact.getName() + ".");
//         } else {
//             view.displayMessage("Your class can't use that artifact. Discarded.");
//         }
//     }
// }