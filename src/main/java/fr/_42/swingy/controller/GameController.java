package fr._42.swingy.controller;

import java.util.List;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.enums.Direction;
import fr._42.swingy.model.map.GameMap;
import fr._42.swingy.model.map.Position;
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

    public GameController(View view, Validator validator, HeroRepository repository) {
        this.view = view;
        this.validator = validator;
        this.repository = repository;
    }

    public void run() {
        heroList = repository.loadAll();
        try {
            currentHero = mainMenu();
            map = new GameMap(mapSizeFor(currentHero.getLevel()));
            map.placeHero(currentHero, map.center());
            gameLoop();
        } finally {
            saveAndExit();
        }
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
    }

    private void handleMove(Direction dir) {
        Position nextPos = map.getNextPosition(currentHero.getPosition(), dir);
        if (map.isInside(nextPos)) {
            currentHero.setPosition(nextPos);
            if (map.hasVillainAt(nextPos)) {
                // Handle battle    
                EncounterResult result = handleEncounter(currentHero, map.getVillainAt(nextPos));
                if (result == EncounterResult.HERO_WON) {
                    view.displayMessage("You defeated the villain!");
                    map.removeVillain(map.getVillainAt(nextPos));
                } else if (result == EncounterResult.HERO_LOST) {
                    view.displayMessage("You were defeated by the villain. Game over.");
                    saveAndExit();
                    System.exit(0);
                }
                else if (result == EncounterResult.HERO_FLED) {
                    view.displayMessage("You fled from the villain.");
                    // Optionally, move the hero back to the previous position
                }
            }
        }
    }

    private Hero mainMenu() {
        view.displayMessage("Welcome to Swingy!");
        view.displayMessage("Available heroes:");
        for (int i = 0; i < heroList.size(); i++) {
            Hero h = heroList.get(i);
            view.displayMessage((i + 1) + ". " + h.getName() + " (Level " + h.getLevel() + ")");
        }
        int choice = Integer.parseInt(view.askInput("Select a hero by number, or 0 to create a new one: ").trim());
        if (choice == 0) {
            return createNewHero();
        } else {
            return heroList.get(choice - 1);
        }
    }

    private Hero createNewHero() {
        String name = view.askInput("Enter hero name: ").trim();
        String className = view.askInput("Enter hero class (e.g., Warrior, Mage): ").trim();
        if (!validator.isValidHeroName(name)) {
            view.displayError("Invalid hero name.");
            return createNewHero();
        }
        if (!validator.isValidHeroClass(className)) {
            view.displayError("Invalid hero class.");
            return createNewHero();
        }
        Hero newHero = new Hero.HeroBuilder(name, className).build();
        heroList.add(newHero);
        return newHero;
    }

    private EncounterResult handleEncounter(Hero hero, Object villain) {
        // Placeholder for encounter logic
        // For now, let's assume the hero always wins
        return EncounterResult.HERO_WON;
    }
}
