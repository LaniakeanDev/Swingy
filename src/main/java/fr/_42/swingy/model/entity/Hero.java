package fr._42.swingy.model.entity;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import fr._42.swingy.model.map.Position;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;


public class Hero {

    @NotNull
    @Size(min = 3, max = 20)
    @Pattern(regexp = "[A-Za-z ]+")
    private final String name;

    @NotNull
    private final HeroClass heroClass;

    @Min(1)
    private int level;

    @Min(0)
    private long experience;

    @Min(0)
    private final int baseHitPoints;

    @Min(0)
    private int currentHitPoints;
    private List<Artifact> artifacts;
    private Position position;

    /* ------------------------------------------------------------------ */
    /*  Getters                                                            */
    /* ------------------------------------------------------------------ */

    public String getName() {
        return name;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public Position getPosition() {
        return position;
    }

    public int getLevel() {
        return level;
    }

    public long getExperience() {
        return experience;
    }

    /** Total attack = class base + level bonus + weapon bonuses. */
    public int getAttack() {
        int base = heroClass.getBaseAttack();
        int levelBonus = (level - 1) * 2;
        int artifactBonus = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.WEAPON)
                .mapToInt(Artifact::getValue)
                .sum();
        return base + levelBonus + artifactBonus;
    }

    /** Total defense = class base + level bonus + armor bonuses. */
    public int getDefense() {
        int base = heroClass.getBaseDefense();
        int levelBonus = (level - 1);
        int artifactBonus = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.ARMOR)
                .mapToInt(Artifact::getValue)
                .sum();
        return base + levelBonus + artifactBonus;
    }

    public int getHitPoints() {
        int levelBonus = (level - 1) * 5;
        int artifactBonus = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.HELM)
                .mapToInt(Artifact::getValue).sum();
        return baseHitPoints + levelBonus + artifactBonus;
    }

    public int getCurrentHitPoints() {
        return currentHitPoints;
    }

    /** Defensive copy so callers can't mutate the internal list. */
    public List<Artifact> getArtifacts() {
        return List.copyOf(artifacts);
    }

    /* ------------------------------------------------------------------ */
    /*  Setters                                                            */
    /* ------------------------------------------------------------------ */

    public void setPosition(Position position) {
        this.position = position;
    }

    /* ------------------------------------------------------------------ */
    /*  Builder pattern                                                   */
    /* ------------------------------------------------------------------ */

    public Hero(HeroBuilder b) {
        this.name      = b.name;
        this.heroClass = b.heroClass;
        this.level     = b.level;
        this.experience = b.experience;
        this.artifacts = b.artifacts;
        this.baseHitPoints = heroClass.getBaseHitPoints();
        // If the builder supplied an HP value, honor it.
        // Otherwise (fresh hero), start at max HP.
        this.currentHitPoints = (b.currentHitPoints >= 0)
                ? b.currentHitPoints
                : getHitPoints();
        this.position = b.position;
    }

    public static class HeroBuilder {
        private String name;
        private HeroClass heroClass;
        private int level = 1;
        private long experience = 0L;
        private int currentHitPoints = -1;         // -1 means "not set"
        private List<Artifact> artifacts = new ArrayList<>();
        private Position position;

        public HeroBuilder position(Position position) {
            this.position = position;
            return this;
        }

        public HeroBuilder name(String name) {
            this.name = name;
            return this;
        }

        public HeroBuilder heroClass(HeroClass heroClass) {
            this.heroClass = heroClass;
            return this;
        }

        public HeroBuilder level(int level) {
            this.level = level;
            return this;
        }

        public HeroBuilder experience(long experience) {
            this.experience = experience;
            return this;
        }

        public HeroBuilder currentHitPoints(int currentHitPoints) {   // ← new
            this.currentHitPoints = currentHitPoints;
            return this;
        }

        public HeroBuilder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public Hero build() {
            return new Hero(this);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Other utilities                                          */
    /* ------------------------------------------------------------------ */

    public void takeDamage(int dmg) {
        currentHitPoints = Math.max(0, currentHitPoints - dmg);
    }

    public void gainExperience(long xp) {
        experience += xp;
        if (experience >= experienceToNextLevel()) {
            levelUp();
        }
    }

    public long experienceToNextLevel() {
        long l = level;
        return l * 1000L + (l - 1) * (l - 1) * 450L;
    }

    private void levelUp() {
        level += 1;
    }

    public boolean equipArtifact(Artifact artifact) {
        if (artifact == null) return false;
        if (!artifact.getType().isCompatibleWith(heroClass)) return false;
        artifacts.add(artifact);
        return true;
    }
}