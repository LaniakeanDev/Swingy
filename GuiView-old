package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import java.util.List;
import java.awt.BorderLayout;


public class GuiView implements View {

    private JFrame frame;
    private JTextArea output;
    private JTextField input;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    public GuiView() {
        try {
            SwingUtilities.invokeAndWait(this::buildUi);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize GUI", e);
        }
    }

    private void buildUi() {
        frame = new JFrame("Swingy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- Output area (the scrolling log / map) ---
        output = new JTextArea(30, 30);
        output.setEditable(false);
        output.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 32));
        output.setMargin(new java.awt.Insets(10, 10, 10, 10));

        // --- Input field ---
        input = new JTextField(80);
        input.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 32));
        input.setMargin(new java.awt.Insets(8, 8, 8, 8));
        input.addActionListener(e -> {
            inputQueue.offer(input.getText());
            input.setText("");
        });

        // --- Labelled input panel ---
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        JLabel prompt = new JLabel("Command:");
        prompt.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 24));
        inputPanel.add(prompt, BorderLayout.WEST);
        inputPanel.add(input,  BorderLayout.CENTER);
        inputPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Assemble the frame ---
        frame.getContentPane().add(new JScrollPane(output), "Center");
        frame.getContentPane().add(inputPanel, "South");

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void showWinDialog(Hero hero) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JLabel message = new JLabel(
                        "<html><div style='text-align:center;'>"
                        + "<h1 style='font-size:48pt;'>Victory</h1>"
                        + "<p style='font-size:32pt;'>Congratulations, " + hero.getName() + "!<br>"
                        + "You reached the border at level " + hero.getLevel() + ".</p>"
                        + "</div></html>");
                message.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));

                // Build the pane manually so we can reach the buttons.
                JOptionPane pane = new JOptionPane(
                        message,
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION);

                JDialog dialog = pane.createDialog(frame, "Victory");

                // Scale every button's font and padding.
                for (java.awt.Component c : pane.getComponents()) {
                    if (c instanceof javax.swing.JPanel) {
                        for (java.awt.Component inner : ((javax.swing.JPanel) c).getComponents()) {
                            if (inner instanceof javax.swing.JButton) {
                                javax.swing.JButton btn = (javax.swing.JButton) inner;
                                btn.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
                                btn.setMargin(new java.awt.Insets(10, 30, 10, 30));
                            }
                        }
                    }
                }

                dialog.pack();                       // re-pack now that the button is bigger
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);             // blocks until dismissed
            });
        } catch (Exception e) {
            displayMessage("Victory!");
        }
    }

    @Override
    public void showLossDialog(Hero hero, Villain villain) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JLabel message = new JLabel(
                        "<html><div style='text-align:center;'>"
                        + "<h1 style='font-size:48pt;'>Game Over</h1>"
                        + "<p style='font-size:32pt;'>You were defeated by " + villain.getName() + "!<br>"
                        + "Better luck next time, " + hero.getName() + ".</p>"
                        + "</div></html>");
                message.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));

                // Build the pane manually so we can reach the buttons.
                JOptionPane pane = new JOptionPane(
                        message,
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION);

                JDialog dialog = pane.createDialog(frame, "Victory");

                // Scale every button's font and padding.
                for (java.awt.Component c : pane.getComponents()) {
                    if (c instanceof javax.swing.JPanel) {
                        for (java.awt.Component inner : ((javax.swing.JPanel) c).getComponents()) {
                            if (inner instanceof javax.swing.JButton) {
                                javax.swing.JButton btn = (javax.swing.JButton) inner;
                                btn.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 36));
                                btn.setMargin(new java.awt.Insets(10, 30, 10, 30));
                            }
                        }
                    }
                }

                dialog.pack();                       // re-pack now that the button is bigger
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);             // blocks until dismissed
            });
        } catch (Exception e) {
            displayMessage("Victory!");
        }
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