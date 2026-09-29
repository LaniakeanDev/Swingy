package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import java.util.List;

public class GuiView implements View {

    private JFrame frame;
    private JTextArea output;
    private JTextField input;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    public GuiView() {
        SwingUtilities.invokeLater(this::buildUi);
    }

    private void buildUi() {
        frame = new JFrame("Swingy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        output = new JTextArea(30, 80);
        output.setEditable(false);
        output.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));

        input = new JTextField();
        input.addActionListener(e -> {
            inputQueue.offer(input.getText());
            input.setText("");
        });

        frame.getContentPane().add(new JScrollPane(output), "Center");
        frame.getContentPane().add(input, "South");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override public void displayMessage(String m) { SwingUtilities.invokeLater(() -> { output.append(m + "\n"); }); }
    @Override public void displayError(String e)   { displayMessage("[!] " + e); }

    @Override
    public String askInput(String prompt) {
        displayMessage("> " + prompt);
        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    @Override
    public void renderMap(GameMap map, Hero hero) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < map.getSize(); y++) {
            for (int x = 0; x < map.getSize(); x++) {
                fr._42.swingy.model.map.Position p =
                        new fr._42.swingy.model.map.Position(x, y);
                if (hero.getPosition() != null && hero.getPosition().equals(p)) sb.append("H ");
                else if (map.getVillainAt(p) != null) sb.append("V ");
                else if (map.isBorder(p)) sb.append("# ");
                else sb.append(". ");
            }
            sb.append("\n");
        }
        displayMessage(sb.toString());
    }

    @Override
    public void showHeroStats(Hero hero) {
        displayMessage(String.format(
                "%s the %s — Lvl %d | XP %d/%d | ATK %d | DEF %d | HP %d/%d",
                hero.getName(), hero.getHeroClass().displayName(),
                hero.getLevel(), hero.getExperience(), hero.experienceToNextLevel(),
                hero.getAttack(), hero.getDefense(),
                hero.getCurrentHitPoints(), hero.getHitPoints()));
    }

    @Override public void displayHeroList(List<Hero> heroes) { /* optional */ }

    @Override
    public void showBattleResult(EncounterResult result, Hero hero, fr._42.swingy.model.entity.Villain villain) {
        displayMessage("Battle: " + result + " vs " + villain.getName());
    }

    @Override public void close() {
        if (frame != null) SwingUtilities.invokeLater(() -> frame.dispose());
    }
}