package cn.loryu.server;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerGUI extends JFrame {
    private final JButton startStopButton;
    private final JLabel statusLabel;
    private final JTextArea logArea;
    private final JTextField portField;
    private Thread serverThread;
    private Server server;

    private final JTable hwidTable;
    private final DefaultTableModel hwidTableModel;
    private final JTextField hwidField;
    private final JTextField noteField; // Field for the note
    private final JTextField searchField; // Field for searching
    private final TableRowSorter<DefaultTableModel> sorter;

    public ServerGUI() {
        setTitle("iLnv Server");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Left panel for controls
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        mainPanel.add(controlPanel, BorderLayout.WEST);

        // Server control panel
        JPanel serverControlPanel = new JPanel(new GridBagLayout());
        serverControlPanel.setBorder(BorderFactory.createTitledBorder("服务器控制"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        serverControlPanel.add(new JLabel("端口:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        portField = new JTextField("5233");
        serverControlPanel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        startStopButton = new JButton("启动服务器");
        serverControlPanel.add(startStopButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        statusLabel = new JLabel("状态: 已停止");
        statusLabel.setForeground(Color.RED);
        serverControlPanel.add(statusLabel, gbc);

        // HWID management panel
        JPanel hwidPanel = new JPanel(new BorderLayout(5, 5));
        hwidPanel.setBorder(BorderFactory.createTitledBorder("HWID管理"));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(25);
        searchPanel.add(new JLabel("搜索:"));
        searchPanel.add(searchField);
        hwidPanel.add(searchPanel, BorderLayout.NORTH);

        // Table for HWIDs
        String[] columnNames = {"HWID", "备注"};
        hwidTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Allow editing
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                String valueStr = (aValue == null) ? "" : aValue.toString().trim();

                // HWID column validation
                if (columnIndex == 0) {
                    if (valueStr.isEmpty()) {
                        JOptionPane.showMessageDialog(ServerGUI.this, "HWID不能为空。", "编辑错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Check for duplicates, excluding the current row
                    for (int i = 0; i < getRowCount(); i++) {
                        if (i == rowIndex) continue;
                        if (getValueAt(i, 0).equals(valueStr)) {
                            JOptionPane.showMessageDialog(ServerGUI.this, "HWID '" + valueStr + "' 已存在。", "编辑错误", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                super.setValueAt(valueStr, rowIndex, columnIndex);
                saveHwidsToServer(); // Auto-save after any successful edit
            }
        };
        hwidTable = new JTable(hwidTableModel);
        sorter = new TableRowSorter<>(hwidTableModel);
        hwidTable.setRowSorter(sorter);

        // Adjust column widths to make remark column smaller
        hwidTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        hwidTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        JScrollPane listScrollPane = new JScrollPane(hwidTable);
        hwidPanel.add(listScrollPane, BorderLayout.CENTER);

        // Panel for adding/removing HWIDs
        JPanel hwidButtonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints bGBC = new GridBagConstraints();
        bGBC.insets = new Insets(2, 2, 2, 2);

        bGBC.gridx = 0; bGBC.gridy = 0; bGBC.weightx = 0; hwidButtonPanel.add(new JLabel("HWID:"), bGBC);
        hwidField = new JTextField();
        bGBC.gridx = 1; bGBC.gridy = 0; bGBC.weightx = 0.7; bGBC.fill = GridBagConstraints.HORIZONTAL; hwidButtonPanel.add(hwidField, bGBC);

        bGBC.gridx = 2; bGBC.gridy = 0; bGBC.weightx = 0; hwidButtonPanel.add(new JLabel("备注:"), bGBC);
        noteField = new JTextField();
        bGBC.gridx = 3; bGBC.gridy = 0; bGBC.weightx = 0.3; bGBC.fill = GridBagConstraints.HORIZONTAL; hwidButtonPanel.add(noteField, bGBC);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("添加");
        JButton removeButton = new JButton("移除");
        actionButtonPanel.add(addButton);
        actionButtonPanel.add(removeButton);

        bGBC.fill = GridBagConstraints.NONE;
        bGBC.anchor = GridBagConstraints.CENTER;
        bGBC.gridx = 0; bGBC.gridy = 1; bGBC.gridwidth = 4; bGBC.weightx = 0; hwidButtonPanel.add(actionButtonPanel, bGBC);

        hwidPanel.add(hwidButtonPanel, BorderLayout.SOUTH);

        // Add panels to control panel
        controlPanel.add(serverControlPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(hwidPanel);


        // Right panel for logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("日志"));
        mainPanel.add(logScrollPane, BorderLayout.CENTER);


        // Redirect System.out and System.err to the JTextArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(logArea), true);
        System.setOut(printStream);
        System.setErr(printStream);

        // Initialize Server instance
        server = new Server(this);

        // Load initial HWIDs
        loadHwidsFromServer();

        // Add search functionality
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        // Add action listeners
        startStopButton.addActionListener(e -> {
            if (serverThread == null || !serverThread.isAlive()) {
                startServer();
            } else {
                stopServer();
            }
        });

        addButton.addActionListener(e -> {
            String newHwid = hwidField.getText();
            String newNote = noteField.getText();
            if (newHwid != null && !newHwid.trim().isEmpty()) {
                String cleanedHwid = newHwid.trim().replaceAll("[^a-zA-Z0-9]", "");
                if (!cleanedHwid.isEmpty()) {
                    // Check for duplicates
                    boolean exists = false;
                    for (int i = 0; i < hwidTableModel.getRowCount(); i++) {
                        if (hwidTableModel.getValueAt(i, 0).equals(cleanedHwid)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        hwidTableModel.addRow(new Object[]{cleanedHwid, newNote.trim()});
                        hwidField.setText("");
                        noteField.setText("");
                        saveHwidsToServer(); // Auto-save
                    } else {
                        JOptionPane.showMessageDialog(this, "HWID已存在。", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "清理后HWID无效。", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "HWID不能为空。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        removeButton.addActionListener(e -> {
            int selectedViewRow = hwidTable.getSelectedRow();
            if (selectedViewRow != -1) {
                int modelRow = hwidTable.convertRowIndexToModel(selectedViewRow);
                hwidTableModel.removeRow(modelRow);
                saveHwidsToServer(); // Auto-save
            } else {
                JOptionPane.showMessageDialog(this, "请选择要移除的HWID。", "警告", JOptionPane.WARNING_MESSAGE);
            }
        });

        /* removeButton.addActionListener(e -> {
            saveHwidsToServer();
        });*/


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (serverThread != null && serverThread.isAlive()) {
                    int confirmed = JOptionPane.showConfirmDialog(ServerGUI.this,
                            "服务器仍在运行。您确定要退出吗?",
                            "退出确认",
                            JOptionPane.YES_NO_OPTION);

                    if (confirmed == JOptionPane.YES_OPTION) {
                        stopServer();
                        dispose();
                        System.exit(0);
                    }
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) {
                JOptionPane.showMessageDialog(this, "无效的端口号。", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Re-initialize server instance before starting
            server = new Server(this);

            serverThread = new Thread(() -> server.start(port));
            serverThread.start();

            statusLabel.setText("状态: 正在运行");
            statusLabel.setForeground(new Color(0, 128, 0)); // Green
            startStopButton.setText("停止服务器");
            portField.setEnabled(false);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "端口号必须是数字。", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop(); // Gracefully stop the server
        }
        if (serverThread != null) {
            serverThread.interrupt(); // Interrupt the thread
            try {
                serverThread.join(5000); // Wait for the thread to die
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        serverStopped();
    }

    public void serverStopped() {
        // This method can be called by the Server thread when it stops unexpectedly
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: 已停止");
            statusLabel.setForeground(Color.RED);
            startStopButton.setText("启动服务器");
            portField.setEnabled(true);
            serverThread = null;
        });
    }

    private void loadHwidsFromServer() {
        server.loadHwids(); // Ensure server loads first
        updateHwidList(server.getHwidMap());
    }

    private void saveHwidsToServer() {
        Map<String, String> hwids = new LinkedHashMap<>();
        for (int i = 0; i < hwidTableModel.getRowCount(); i++) {
            String hwid = (String) hwidTableModel.getValueAt(i, 0);
            String note = (String) hwidTableModel.getValueAt(i, 1);
            hwids.put(hwid, note);
        }
        server.saveHwids(hwids);
    }

    public void updateHwidList(Map<String, String> hwids) {
        SwingUtilities.invokeLater(() -> {
            hwidTableModel.setRowCount(0); // Clear table
            for (Map.Entry<String, String> entry : hwids.entrySet()) {
                hwidTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        });
    }

    private void filterTable() {
        String searchText = searchField.getText();
        if (searchText.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            // Case-insensitive search in all columns
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    public static void main(String[] args) {
        // Set a more modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }

    // Custom OutputStream to redirect console output to the JTextArea
    public static class CustomOutputStream extends OutputStream {
        private final JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            final String text = new String(b, off, len);
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }

        @Override
        public void write(int b) {
            write(new byte[]{(byte) b}, 0, 1);
        }
    }
}