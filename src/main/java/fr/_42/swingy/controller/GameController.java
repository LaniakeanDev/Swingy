package fr._42.swingy.controller;
import java.util.Random;
import java.util.List;

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
import fr._42.swingy.persistence.HeroRepository;
import fr._42.swingy.validation.Validator;
import fr._42.swingy.view.View;

public class GameController {

    private final View view;
    private final Validator validator;
    private final HeroRepository repository;
    private GameMap map;
    private Hero currentHero;
    private List<Hero> heroList;
    private final Random random = new Random();
    private final BattleSimulator battleSimulator = new BattleSimulator(random);

    public GameController(View view, Validator validator, HeroRepository repository) {
        this.view = view;
        this.validator = validator;
        this.repository = repository;
    }

    public void run() {
        heroList = repository.loadAll();
        try {
            while ((currentHero = mainMenu()) == null) {
                view.displayMessage("No hero selected. Let's try again.");
            }
            int size = mapSizeFor(currentHero.getLevel());
            map = new GameMap(size);
            currentHero.setPosition(map.center());
            map.placeHero(currentHero, map.center());
            map.generateVillains(villainCountFor(size), currentHero.getLevel());
            gameLoop();
        } finally {
            saveAndExit();
        }
    }

    private int villainCountFor(int mapSize) {
        return Math.max(2, mapSize / 3);
    }

    private void gameLoop() {
        boolean running = true;
        while (running) {
            view.renderMap(map, currentHero);
            view.showHeroStats(currentHero);

            String input = view.askInput("Move (north/east/south/west) or 'exit': ").trim().toLowerCase();

            if ("exit".equals(input)) {
                running = false;
                continue;
            }

            Direction dir = Direction.fromString(input);   // returns null if invalid
            if (dir == null) {
                view.displayError("Invalid direction: '" + input + "'");
                continue;
            }

            handleMove(dir);

            if (currentHero.getPosition() != null && map.isBorder(currentHero.getPosition())) {
                view.displayMessage("You reached the border — you win!");
                running = false;
            }
        }
    }

    private int mapSizeFor(int level) {
        return (level - 1) * 5 + 10 - (level % 2);
    }

    private void saveAndExit() {
        repository.saveAll(heroList);
        view.displayMessage("Your hero(es) have been saved");
    }

    private void handleMove(Direction dir) {
        Position current = currentHero.getPosition();
        Position next    = map.getNextPosition(current, dir);

        if (map.isBorder(next)) {
            view.displayMessage("You reached the border — you win!");
            saveAndExit();
            System.exit(0);
        }

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
                case HERO_FLED -> view.displayMessage(
                    "You fled. The villain still blocks the path.");
                case HERO_LOST -> {
                    view.displayMessage("You were defeated. Game over.");
                    saveAndExit();
                    System.exit(0);
                }
            }
            return;
        }
        currentHero.setPosition(next);
    }

    private Hero mainMenu() {
        view.displayMessage("Welcome to Swingy!");

        while (true) {
            view.displayMessage("Available heroes:");
            for (int i = 0; i < heroList.size(); i++) {
                Hero h = heroList.get(i);
                view.displayMessage("  " + (i + 1) + ". "
                        + h.getName() + " (Level " + h.getLevel() + ")");
            }

            String input = view.askInput("Select a hero by number, or 0 to create a new one: ").trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                view.displayError("Please enter a number.");
                continue;
            }

            if (choice == 0) {
                return createNewHero();
            }
            if (choice < 1 || choice > heroList.size()) {
                view.displayError("No hero with number " + choice + ".");
                continue;
            }
            return heroList.get(choice - 1);
        }
    }

    private String askHeroClass() {
        while (true) {
            view.displayMessage("Available hero classes:");
            for (HeroClass hc : HeroClass.values()) {
                view.displayMessage("  - " + hc.name());
            }
            String className = view.askInput("Enter hero class: ").trim();
            HeroClass heroClass = HeroClass.fromString(className);
            if (heroClass != null) {
                return className;
            }
            view.displayError("Unknown hero class: '" + className + "'");
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
        while (true) {
            String name = view.askInput("Enter hero name: ").trim();
            while (!checkNameUnicity(name)) {
                view.displayMessage("Hero name already taken. Please choose another.");
                name = view.askInput("Enter hero name: ").trim();
            }

            HeroClass heroClass = HeroClass.fromString(askHeroClass());
            Hero newHero = new Hero.HeroBuilder()
                    .name(name)
                    .heroClass(heroClass)
                    .build();

            if (!validator.isValid(newHero)) {
                view.displayError(validator.validateAndCollect(newHero));
                continue;
            }

            heroList.add(newHero);
            return newHero;
        }
    }

    private EncounterResult handleEncounter(Hero hero, Villain villain) {
        view.displayMessage(String.format(
            "A %s (power %d) blocks your path!", villain.getName(), villain.getAttack()));

        String choice = view.askInput("Fight or Run? [f/r]: ").trim().toLowerCase();
        if (choice.startsWith("r")) {
            if (random.nextBoolean()) {              // ← still uses `random`
                return EncounterResult.HERO_FLED;
            }
            view.displayMessage("You failed to escape — you must fight!");
        }

        BattleReport report = battleSimulator.fight(hero, villain);

        // Narrate every exchange
        report.log().forEach(view::displayMessage);

        // Handle the drop, if any
        report.drop().ifPresent(artifact -> promptKeepArtifact(hero, artifact));

        return report.result();
    }

    private void promptKeepArtifact(Hero hero, Artifact artifact) {
        String keep = view.askInput("Keep it? [y/n]: ").trim().toLowerCase();
        if (!keep.startsWith("y")) {
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
