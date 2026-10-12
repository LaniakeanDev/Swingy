package fr._42.swingy.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Two-row action bar:
 *   - top row: primary commands (Create New Hero, Quit, ...)
 *   - bottom row: contextual choices (hero selection, in-game actions)
 * Each button click pushes its label into the command consumer.
 */
class ActionBar extends JPanel {

    private final Consumer<String> onCommand;
    private final JPanel primaryRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
    private final JPanel choiceRow  = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));

    ActionBar(Consumer<String> onCommand) {
        this.onCommand = onCommand;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        primaryRow.setOpaque(false);
        choiceRow.setOpaque(false);

        add(primaryRow);
        add(Box.createVerticalStrut(4));
        add(choiceRow);
    }

    void setPrimaryActions(List<String> labels) {
        primaryRow.removeAll();
        for (String label : labels) {
            primaryRow.add(makeButton(label, true));
        }
        primaryRow.revalidate();
        primaryRow.repaint();
    }

    void setChoices(List<String> labels) {
        choiceRow.removeAll();
        for (String label : labels) {
            choiceRow.add(makeButton(label, false));
        }
        choiceRow.revalidate();
        choiceRow.repaint();
    }

    private JButton makeButton(String label, boolean primary) {
        JButton b = new JButton(label);
        b.setFont(new Font("SansSerif", primary ? Font.BOLD : Font.PLAIN, primary ? 16 : 15));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setFocusPainted(false);
        if (primary) {
            b.setBackground(new Color(60, 120, 200));
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
            b.setBorderPainted(false);
        }
        b.addActionListener(e -> onCommand.accept(label));
        return b;
    }
}