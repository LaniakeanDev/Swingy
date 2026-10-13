// src/main/java/fr/_42/swingy/view/InventoryPanel.java
package fr._42.swingy.view;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of the hero's equipped artifacts.
 *
 * Renders a small header (name, class, level) plus a table of artifacts
 * with columns: Type, Name, Value. Also shows a derived "Total bonus"
 * per type at the bottom so the player can see how their build stacks up.
 */
public class InventoryPanel extends JPanel {

    private final JLabel header = new JLabel();
    private final ArtifactTableModel model = new ArtifactTableModel();
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel();

    public InventoryPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(25, 25, 32));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- Header ----
        header.setFont(new Font("Serif", Font.BOLD, 22));
        header.setForeground(new Color(230, 200, 100));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(header, BorderLayout.NORTH);

        // ---- Table ----
        table.setFont(new Font("Monospaced", Font.PLAIN, 14));
        table.setRowHeight(26);
        table.setBackground(new Color(20, 20, 25));
        table.setForeground(new Color(220, 220, 220));
        table.setGridColor(new Color(60, 60, 60));
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setEnabled(false);   // read-only

        // Centre-align the Value column, left-align the rest.
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(0).setCellRenderer(left);
        table.getColumnModel().getColumn(1).setCellRenderer(left);
        table.getColumnModel().getColumn(2).setCellRenderer(right);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20, 20, 25));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        add(scroll, BorderLayout.CENTER);

        // ---- Summary ----
        summary.setFont(new Font("Monospaced", Font.PLAIN, 13));
        summary.setForeground(new Color(180, 220, 180));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(summary, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(420, 320));
    }

    /** Refresh the panel from the given hero. Safe to call from the EDT. */
    public void update(Hero hero) {
        if (hero == null) {
            header.setText("Inventory");
            model.setArtifacts(List.of());
            summary.setText("");
            return;
        }

        header.setText("Inventory");

        List<Artifact> artifacts = hero.getArtifacts();
        model.setArtifacts(artifacts == null ? List.of() : artifacts);

        // Per-type totals.
        java.util.Map<String, Integer> totals = new java.util.LinkedHashMap<>();
        if (artifacts != null) {
            for (Artifact a : artifacts) {
                totals.merge(a.getType().name(), a.getValue(), Integer::sum);
            }
        }
        if (totals.isEmpty()) {
            summary.setText("No artifacts equipped.");
        } else {
            StringBuilder sb = new StringBuilder("Total bonuses: ");
            boolean first = true;
            for (var e : totals.entrySet()) {
                if (!first) sb.append("   ");
                sb.append(e.getKey()).append(" +").append(e.getValue());
                first = false;
            }
            summary.setText(sb.toString());
        }
    }

    // ---------------------------------------------------------------
    // Table model
    // ---------------------------------------------------------------

    private static class ArtifactTableModel extends AbstractTableModel {
        private final String[] cols = {"Type", "Name", "Value"};
        private List<Artifact> rows = new ArrayList<>();

        void setArtifacts(List<Artifact> artifacts) {
            this.rows = new ArrayList<>(artifacts);
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            Artifact a = rows.get(row);
            return switch (col) {
                case 0 -> a.getType().name();
                case 1 -> a.getName();
                case 2 -> "+" + a.getValue();
                default -> "";
            };
        }
    }
}