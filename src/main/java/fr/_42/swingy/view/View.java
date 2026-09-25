package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;

public interface View {
    void displayMessage(String message);
    void displayError(String error);
    String askInput(String prompt);
    void renderMap(GameMap map, Hero hero);
    void showHeroStats(Hero hero);
    void showBattleResult(EncounterResult result);
    void close();
}
