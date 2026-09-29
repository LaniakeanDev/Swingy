package fr._42.swingy.model.entity;

import fr._42.swingy.model.map.Position;

public class Villain {
    private final String name;
    private int hitPoints;
    private int attack;
    private int defense;
    private Position position;

    public Villain(String name, int hitPoints, int attack, int defense, Position position) {
        this.name = name;
        this.hitPoints = hitPoints;
        this.attack = attack;
        this.defense = defense;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public Position getPosition() {
        return position;
    }

    public void takeDamage(int dmg) {
        hitPoints = Math.max(0, hitPoints - dmg);
    }
}
