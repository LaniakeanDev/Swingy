package fr._42.swingy.model.entity;

import java.util.ArrayList;
import java.util.List;

import fr._42.swingy.model.map.Position;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;


public class Hero {

    private final String name;
    private final HeroClass heroClass;

    private int level;
    private long experience;
    private int hitPoints;
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

    /** Max hit points = class base + level bonus + helm bonuses. */
    public int getHitPoints() {
        int base = hitPoints;
        int levelBonus = (level - 1) * 5;
        int artifactBonus = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.HELM)
                .mapToInt(Artifact::getValue)
                .sum();
        return base + levelBonus + artifactBonus;
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
        this.hitPoints = heroClass.getBaseHitPoints();
        this.artifacts = b.artifacts;
    }

    public static class HeroBuilder {
        private String name;
        private HeroClass heroClass;
        private int level = 1;
        private long experience = 0L;
        private List<Artifact> artifacts = new ArrayList<>();

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
        hitPoints -= dmg;
    }

    public void gainExperience(long xp) {
        experience += xp;
        if (experience >= experienceToNextLevel()) {
            levelUp();
        }
    }

    public int experienceToNextLevel() {
        return level * 100;
    }

    private void levelUp() {
        level += 1;
    }

    public boolean equipArtifact(Artifact artifact) {
        if (artifact.getType() == ArtifactType.WEAPON) {
            artifacts.add(artifact);
            return true;
        } else if (artifact.getType() == ArtifactType.ARMOR) {
            artifacts.add(artifact);
            return true;
        } else if (artifact.getType() == ArtifactType.HELM) {
            artifacts.add(artifact);
            return true;
        }
        return false;
    }
}