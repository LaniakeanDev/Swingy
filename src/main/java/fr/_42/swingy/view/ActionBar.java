package fr._42.swingy.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ActionBar extends JPanel {

    public interface Sink { void submit(String command); }

    private final Sink sink;

    public ActionBar(Sink sink) {
        this.sink = sink;
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public void setActions(List<String> labels) {
        removeAll();
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 8));
        for (String label : labels) {
            add(makeButton(label));
        }
        revalidate();
        repaint();
    }

    /** One big button per hero, arranged in a grid so many fit on screen. */
    public void setChoices(List<String> labels) {
        removeAll();
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(labels.size())));
        setLayout(new GridLayout(0, cols, 10, 10));
        for (String label : labels) {
            JButton b = makeButton(label);
            b.setFont(new Font("Monospaced", Font.BOLD, 18));
            add(b);
        }
        revalidate();
        repaint();
    }

    private JButton makeButton(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Monospaced", Font.BOLD, 20));
        b.setMargin(new Insets(8, 20, 8, 20));
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> sink.submit(label));
        return b;
    }
}