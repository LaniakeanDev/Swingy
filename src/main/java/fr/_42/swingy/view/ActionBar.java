package fr._42.swingy.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Two-section action bar:
 *   - top: primary commands (Create New Hero, Quit, ...) as a horizontal row
 *   - bottom: contextual choices (hero selection, in-game actions) as a centred column
 * Each button click pushes its label into the command consumer.
 */
class ActionBar extends JPanel {

    private final Consumer<String> onCommand;

    private final JPanel primaryRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));

    // Column wrapper: FlowLayout(CENTER) keeps the column horizontally centred;
    // GridLayout(0,1,gap,gap) stacks the buttons with a uniform vertical gap.
    private JPanel choiceColumn;
    private final JPanel choiceWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    ActionBar(Consumer<String> onCommand, boolean verticalLayout) {
        this.onCommand = onCommand;
        if (verticalLayout) {
            choiceColumn = new JPanel(new GridLayout(0, 1, 0, 8));
        } else {
            choiceColumn = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        }
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        primaryRow.setOpaque(false);
        choiceColumn.setOpaque(false);
        choiceWrapper.setOpaque(false);

        choiceWrapper.add(choiceColumn);

        add(primaryRow);
        add(Box.createVerticalStrut(8));
        add(choiceWrapper);
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
        choiceColumn.removeAll();
        for (String label : labels) {
            choiceColumn.add(makeButton(label, false));
        }
        choiceColumn.revalidate();
        choiceColumn.repaint();
    }

    private JButton makeButton(String label, boolean primary) {
        String displayLabel = label;
        if (label == "North") {
            displayLabel = "↑";
        }
        else if (label == "South") {
            displayLabel = "↓";
        }
        else if (label == "West") {
            displayLabel = "←";
        }
        else if (label == "East") {
            displayLabel = "→";
        }
        JButton b = new JButton(displayLabel);
        b.setFont(new Font("SansSerif", primary ? Font.BOLD : Font.PLAIN, primary ? 16 : 15));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setFocusPainted(true);
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