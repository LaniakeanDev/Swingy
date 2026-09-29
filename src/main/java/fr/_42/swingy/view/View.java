package fr._42.swingy.view;

import java.util.List;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;

public interface View {
    void displayMessage(String message);
    void displayError(String error);
    String askInput(String prompt);
    void renderMap(GameMap map, Hero hero);
    void showHeroStats(Hero hero);
    void displayHeroList(List<Hero> heroes);
    void showBattleResult(EncounterResult result, Hero hero, Villain villain);
    void close();
}
