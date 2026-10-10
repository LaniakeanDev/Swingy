package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.map.GameMap;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuiView implements View {

    private JFrame frame;
    private MapPanel mapPanel;
    private JTextArea log;
    private ActionBar actionBar;

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

        // Map on top (scrollable in case the map is bigger than the window)
        mapPanel = new MapPanel();
        JScrollPane mapScroll = new JScrollPane(mapPanel);
        mapScroll.setPreferredSize(new Dimension(640, 480));
        mapScroll.getViewport().setBackground(Color.BLACK);

        // Log below the map
        log = new JTextArea(10, 60);
        log.setEditable(false);
        log.setFont(new Font("Monospaced", Font.PLAIN, 14));
        log.setMargin(new Insets(6, 6, 6, 6));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(1280, 200));

        // Buttons for input
        actionBar = new ActionBar(cmd -> inputQueue.offer(cmd));

        // Layout: map center, log south, actions at the very bottom
        JPanel south = new JPanel(new BorderLayout());
        south.add(logScroll,   BorderLayout.CENTER);
        south.add(actionBar,   BorderLayout.SOUTH);

        frame.getContentPane().add(mapScroll, BorderLayout.CENTER);
        frame.getContentPane().add(south,      BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(1280, 900);
        frame.setMinimumSize(new Dimension(900, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------------------------------------------------------------
    // View interface
    // ---------------------------------------------------------------

    @Override
    public void renderMap(GameMap map, Hero hero) {
        SwingUtilities.invokeLater(() -> mapPanel.setState(map, hero));
    }

    @Override
    public void displayMessage(String m) {
        SwingUtilities.invokeLater(() -> log.append(m + "\n"));
    }

    @Override
    public void displayError(String e) {
        displayMessage("[!] " + e);
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

    /**
     * Pushes button labels into the action bar, then blocks until the user
     * clicks one. The game loop continues to use askInput as before.
     */
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
    public String askText(String prompt) {
        // Must run on the EDT, but showInputDialog blocks the calling (game) thread
        // which is exactly what we want.
        final String[] result = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                result[0] = JOptionPane.showInputDialog(
                        frame, prompt, "Input", JOptionPane.PLAIN_MESSAGE);
            });
        } catch (Exception e) {
            return null;
        }
        return result[0];
    }

    // ---------------------------------------------------------------
    // NEW: tell the action bar which buttons to show
    // ---------------------------------------------------------------

    /** Optional API: call this from the controller whenever the state changes. */
    public void setActions(List<String> labels) {
        SwingUtilities.invokeLater(() -> actionBar.setActions(labels));
    }

    // ---------------------------------------------------------------
    // Dialogs (unchanged from your original except title fix)
    // ---------------------------------------------------------------

    @Override
    public void showWinDialog(Hero hero) {
        showDialog("Victory",
                   "Congratulations, " + hero.getName() + "!<br>"
                   + "You reached the border at level " + hero.getLevel() + ".");
    }

    @Override
    public void showLossDialog(Hero hero, Villain villain) {
        showDialog("Game Over",
                   "You were defeated by " + villain.getName() + "!<br>"
                   + "Better luck next time, " + hero.getName() + ".");
    }

    private void showDialog(String title, String htmlBody) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JLabel message = new JLabel(
                        "<html><div style='text-align:center;'>"
                        + "<h1 style='font-size:48pt;'>" + title + "</h1>"
                        + "<p style='font-size:32pt;'>" + htmlBody + "</p>"
                        + "</div></html>");
                message.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

                JOptionPane pane = new JOptionPane(
                        message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
                JDialog dialog = pane.createDialog(frame, title);

                // Scale every button
                for (Component c : pane.getComponents()) {
                    if (c instanceof JPanel) {
                        for (Component inner : ((JPanel) c).getComponents()) {
                            if (inner instanceof JButton) {
                                JButton btn = (JButton) inner;
                                btn.setFont(new Font("Monospaced", Font.BOLD, 24));
                                btn.setMargin(new Insets(10, 30, 10, 30));
                            }
                        }
                    }
                }
                dialog.pack();
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);
            });
        } catch (Exception e) {
            displayMessage(title);
        }
    }

    @Override public void displayHeroList(List<Hero> heroes) { /* optional */ }

    @Override
    public void showBattleResult(EncounterResult result, Hero hero, Villain villain) {
        displayMessage("Battle: " + result + " vs " + villain.getName());
    }

    @Override
    public void close() {
        if (frame != null) SwingUtilities.invokeLater(() -> frame.dispose());
    }

    /** Show one button per hero; askInput() will return the chosen label. */
    public void setHeroChoices(List<String> labels) {
        SwingUtilities.invokeLater(() -> actionBar.setChoices(labels));
    }
}















