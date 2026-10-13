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

    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";

    private JFrame frame;
    private CardLayout cards;
    private JPanel root;

    // Menu screen
    private JPanel menuPanel;
    private ActionBar menuActions;

    // Game screen
    private MapPanel mapPanel;
    private StatsPanel statsPanel;
    private JTextArea log;
    private ActionBar gameActions;
    private InventoryPanel inventoryPanel;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    public GuiView() {
        try {
            SwingUtilities.invokeAndWait(this::buildUi);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize GUI", e);
        }
    }

    // ---------------------------------------------------------------
    // UI construction
    // ---------------------------------------------------------------

    private void buildUi() {
        frame = new JFrame("Culture Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cards = new CardLayout();
        root  = new JPanel(cards);

        menuPanel = buildMenuPanel();
        root.add(menuPanel, CARD_MENU);

        root.add(buildGamePanel(), CARD_GAME);

        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setPreferredSize(new Dimension(1280, 860));
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);

        cards.show(root, CARD_MENU);
    }

    private JPanel buildMenuPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(30, 30, 40));

        JLabel title = new JLabel("Culture Swing", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 72));
        title.setForeground(new Color(230, 200, 100));
        title.setBorder(BorderFactory.createEmptyBorder(60, 0, 40, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);

        menuActions = new ActionBar(cmd -> inputQueue.offer(cmd), true);
        center.add(menuActions, BorderLayout.CENTER);

        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildGamePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());

        // -- Center: map + side stats --
        mapPanel = new MapPanel();
        JScrollPane mapScroll = new JScrollPane(mapPanel);
        mapScroll.getViewport().setBackground(Color.BLACK);
        mapScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));

        statsPanel = new StatsPanel();
        // statsPanel.setPreferredSize(new Dimension(260, 0));

        inventoryPanel = new InventoryPanel();

        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(500, 0));
        side.add(statsPanel,     BorderLayout.NORTH);
        side.add(inventoryPanel, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.add(mapScroll, BorderLayout.CENTER);
        center.add(side,      BorderLayout.EAST);

        // -- Log --
        log = new JTextArea(6, 60);
        log.setEditable(false);
        log.setFont(new Font("Monospaced", Font.PLAIN, 14));
        log.setMargin(new Insets(6, 6, 6, 6));
        log.setBackground(new Color(20, 20, 25));
        log.setForeground(new Color(220, 220, 220));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        // -- Actions --
        gameActions = new ActionBar(cmd -> inputQueue.offer(cmd), false);

        JPanel south = new JPanel(new BorderLayout());
        south.add(logScroll,   BorderLayout.CENTER);
        south.add(gameActions, BorderLayout.SOUTH);

        wrapper.add(center, BorderLayout.CENTER);
        wrapper.add(south,  BorderLayout.SOUTH);
        return wrapper;
    }

    // ---------------------------------------------------------------
    // Screen switching (call from controller)
    // ---------------------------------------------------------------

    public void showMenu() {
        SwingUtilities.invokeLater(() -> {
            menuActions.setChoices(List.of());
            menuActions.setPrimaryActions(List.of());
            cards.show(root, CARD_MENU);
        });
    }

    public void showGame() {
        SwingUtilities.invokeLater(() -> cards.show(root, CARD_GAME));
    }

    // ---------------------------------------------------------------
    // View interface
    // ---------------------------------------------------------------

    @Override
    public void renderMap(GameMap map, Hero hero) {
        SwingUtilities.invokeLater(() -> {
            mapPanel.setState(map, hero);
            statsPanel.update(hero);
            inventoryPanel.update(hero);
        });
    }

    @Override
    public void displayMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            log.append(m + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    @Override
    public void displayError(String e) {
        displayMessage("[!] " + e);
    }

    @Override
    public void showHeroStats(Hero hero) {
        SwingUtilities.invokeLater(() -> {
            statsPanel.update(hero);
            inventoryPanel.update(hero);
        });
    }

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

    /**
     * Purges any button clicks that were enqueued but never consumed.
     * Called by the controller right before each blocking prompt so that
     * a stray click from a previous screen can't be picked up by the next one.
     */
    public void drainInput() {
        inputQueue.clear();
    }

    @Override
    public String askText(String prompt) {
        final String[] result = new String[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                result[0] = JOptionPane.showInputDialog(
                        frame, prompt, "Input", JOptionPane.PLAIN_MESSAGE));
        } catch (Exception e) {
            return null;
        }
        return result[0];
    }

    // ---------------------------------------------------------------
    // Action bar API
    // ---------------------------------------------------------------

    /** Primary commands (Create New Hero, Quit, Back, …) — menu screen. */
    public void setMenuPrimaryActions(List<String> labels) {
        SwingUtilities.invokeLater(() -> menuActions.setPrimaryActions(labels));
    }

    /** Hero choices (or any secondary options) — menu screen. */
    public void setMenuChoices(List<String> labels) {
        SwingUtilities.invokeLater(() -> menuActions.setChoices(labels));
    }

    /** Primary commands on the game screen (Save, Quit, Back to menu, …). */
    public void setGamePrimaryActions(List<String> labels) {
        SwingUtilities.invokeLater(() -> gameActions.setPrimaryActions(labels));
    }

    /** In-game context choices (Move N/S/E/W, Fight, Run, …). */
    public void setGameChoices(List<String> labels) {
        SwingUtilities.invokeLater(() -> gameActions.setChoices(labels));
    }

    // ---------------------------------------------------------------
    // Dialogs
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

                for (Component c : pane.getComponents()) {
                    if (c instanceof JPanel) {
                        for (Component inner : ((JPanel) c).getComponents()) {
                            if (inner instanceof JButton btn) {
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

    // Kept for backwards compat — routes to menu choices
    public void setHeroChoices(List<String> labels) { setMenuChoices(labels); }
}