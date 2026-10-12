package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;

import javax.swing.*;
import java.awt.*;

class StatsPanel extends JPanel {

    private final JLabel name    = new JLabel("—");
    private final JLabel cls     = new JLabel("—");
    private final JLabel level   = new JLabel("—");
    private final JLabel xp      = new JLabel("—");
    private final JLabel attack  = new JLabel("—");
    private final JLabel defense = new JLabel("—");
    private final JLabel hp      = new JLabel("—");

    StatsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Hero"));
        setBackground(new Color(245, 245, 245));

        Font labelFont = new Font("SansSerif", Font.PLAIN, 14);
        Font valueFont = new Font("Monospaced", Font.BOLD, 15);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        addRow(c, 0, "Name",     name,     labelFont, valueFont);
        addRow(c, 1, "Class",    cls,      labelFont, valueFont);
        addRow(c, 2, "Level",    level,    labelFont, valueFont);
        addRow(c, 3, "XP",       xp,       labelFont, valueFont);
        addRow(c, 4, "Attack",   attack,   labelFont, valueFont);
        addRow(c, 5, "Defense",  defense,  labelFont, valueFont);
        addRow(c, 6, "HP",       hp,       labelFont, valueFont);
    }

    private void addRow(GridBagConstraints c, int row, String labelText,
                        JLabel value, Font lf, Font vf) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(labelText + ":");
        l.setFont(lf);
        add(l, c);

        c.gridx = 1; c.weightx = 1;
        value.setFont(vf);
        add(value, c);
    }

    void update(Hero hero) {
        if (hero == null) {
            name.setText("—"); cls.setText("—"); level.setText("—");
            xp.setText("—"); attack.setText("—"); defense.setText("—"); hp.setText("—");
            return;
        }
        name.setText(hero.getName());
        cls.setText(hero.getHeroClass().displayName());
        level.setText(String.valueOf(hero.getLevel()));
        xp.setText(hero.getExperience() + " / " + hero.experienceToNextLevel());
        attack.setText(String.valueOf(hero.getAttack()));
        defense.setText(String.valueOf(hero.getDefense()));
        hp.setText(hero.getCurrentHitPoints() + " / " + hero.getHitPoints());
    }
}