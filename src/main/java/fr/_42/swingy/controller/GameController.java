package fr._42.swingy.controller;
import java.util.Random;
import java.util.List;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.ArtifactPool;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.enums.HeroClass;
// import fr._42.swingy.model.enums.ArtifactType;
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
    private final Random random = new Random();

    public GameController(View view, Validator validator, HeroRepository repository) {
        this.view = view;
        this.validator = validator;
        this.repository = repository;
    }

    public void run() {
        heroList = repository.loadAll();
        try {
            currentHero = mainMenu();
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
        HeroClass heroClass = HeroClass.fromString(className);
        Hero newHero = new Hero.HeroBuilder()
            .name(name)
            .heroClass(heroClass)
            .build();
        heroList.add(newHero);
        return newHero;
    }

    private EncounterResult handleEncounter(Hero hero, Villain villain) {
        view.displayMessage(String.format(
            "A %s (power %d) blocks your path!", villain.getName(), villain.getAttack()));

        String choice = view.askInput("Fight or Run? [f/r]: ").trim().toLowerCase();
        if (choice.startsWith("r")) {
            if (random.nextBoolean()) {
                return EncounterResult.HERO_FLED;
            }
            view.displayMessage("You failed to escape — you must fight!");
        }

        return simulateBattle(hero, villain);
    }

    /**
     * Turn-based duel. Damage = max(1, attack - defense/2) with a
     * ±20% luck multiplier. Hero strikes first.
     */
    private EncounterResult simulateBattle(Hero hero, Villain villain) {
        int heroHp    = hero.getHitPoints();
        int villainHp = villain.getHitPoints();

        view.displayMessage(String.format(
            "Battle begins! Hero %d HP vs Villain %d HP", heroHp, villainHp));

        boolean heroTurn = true;
        while (heroHp > 0 && villainHp > 0) {
            if (heroTurn) {
                int dmg = computeDamage(hero.getAttack(), villain.getDefense());
                villainHp -= dmg;
                view.displayMessage(String.format(
                    "  You hit for %d. Villain HP: %d", dmg, Math.max(0, villainHp)));
            } else {
                int dmg = computeDamage(villain.getAttack(), hero.getDefense());
                heroHp -= dmg;
                view.displayMessage(String.format(
                    "  Villain hits for %d. Your HP: %d", dmg, Math.max(0, heroHp)));
            }
            heroTurn = !heroTurn;
        }

        if (heroHp > 0) {
            hero.takeDamage(hero.getHitPoints() - heroHp);
            applyVictoryRewards(hero, villain);
            return EncounterResult.HERO_WON;
        }

        hero.takeDamage(hero.getHitPoints());
        return EncounterResult.HERO_LOST;
    }

    /** damage = max(1, attack - defense/2) * random[0.8, 1.2) */
    private int computeDamage(int attack, int defense) {
        int base = Math.max(1, attack - defense / 2);
        double luck = 0.8 + random.nextDouble() * 0.4;
        return Math.max(1, (int) Math.round(base * luck));
    }

    /** XP = power * 100; 40% chance of an artifact scaled to villain power. */
    private void applyVictoryRewards(Hero hero, Villain villain) {
        // --- XP ---
        long xp = villain.getAttack() * 100L;
        hero.gainExperience(xp);
        view.displayMessage(String.format(
            "You gain %d XP. (Level %d, %d XP to next)",
            xp, hero.getLevel(), hero.experienceToNextLevel()));

        // --- Artifact drop ---
        if (random.nextDouble() >= 0.60) {
            view.displayMessage("No artifact dropped.");
            return;
        }

        Artifact artifact = ArtifactPool.rollForPower(random, villain.getAttack());
        view.displayMessage(String.format(
            "The villain dropped: %s (+%d %s).",
            artifact.getName(),
            artifact.getValue(),
            artifact.getType().displayName().toLowerCase()));

        String keep = view.askInput("Keep it? [y/n]: ").trim().toLowerCase();
        if (keep.startsWith("y")) {
            if (hero.equipArtifact(artifact)) {
                view.displayMessage("Equipped " + artifact.getName() + ".");
            } else {
                view.displayMessage("Your class can't use that artifact. Discarded.");
            }
        } else {
            view.displayMessage("You leave it behind.");
        }
    }
}
