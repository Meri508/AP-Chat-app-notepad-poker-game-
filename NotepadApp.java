import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NotepadApp extends JFrame {
    private final JTextArea editor = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel countLabel = new JLabel("0 words | 0 characters");
    private final JFileChooser fileChooser = new JFileChooser();

    private Path currentFile;
    private boolean changed = false;

    public NotepadApp() {
        setTitle("Nova Notepad");
        setSize(900, 600);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "md", "log", "java"));

        buildInterface();
        setupEvents();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(238, 242, 247));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Nova Notepad");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(31, 41, 55));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);
        toolbar.add(button("New", e -> newFile()));
        toolbar.add(button("Open", e -> openFile()));
        toolbar.add(button("Save", e -> saveFile()));
        toolbar.add(button("Save As", e -> saveFileAs()));
        toolbar.add(button("Clear", e -> editor.setText("")));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(toolbar, BorderLayout.EAST);

        editor.setFont(new Font("Consolas", Font.PLAIN, 16));
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setBorder(new EmptyBorder(20, 20, 20, 20));
        editor.setForeground(new Color(31, 41, 55));
        editor.setCaretColor(new Color(37, 99, 235));

        JScrollPane scrollPane = new JScrollPane(editor);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 2, 0, 2));
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(countLabel, BorderLayout.EAST);

        root.add(top, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JButton button(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(31, 41, 55));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(8, 14, 8, 14)
        ));
        return button;
    }

    private void setupEvents() {
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                markChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                markChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                markChanged();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (confirmDiscard()) {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void markChanged() {
        changed = true;
        statusLabel.setText("Unsaved changes");

        String text = editor.getText().trim();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        int characters = editor.getText().length();

        countLabel.setText(words + " words | " + characters + " characters");
    }

    private void newFile() {
        if (!confirmDiscard()) return;

        editor.setText("");
        currentFile = null;
        changed = false;
        statusLabel.setText("New note");
        setTitle("Nova Notepad");
    }

    private void openFile() {
        if (!confirmDiscard()) return;

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                editor.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                currentFile = file.toPath();
                changed = false;
                statusLabel.setText("Opened " + file.getName());
                setTitle("Nova Notepad - " + file.getName());
            } catch (IOException e) {
                showError("Could not open file.");
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }

        writeFile(currentFile);
    }

    private void saveFileAs() {
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();

            if (!path.getFileName().toString().contains(".")) {
                path = path.resolveSibling(path.getFileName() + ".txt");
            }

            writeFile(path);
        }
    }

    private void writeFile(Path path) {
        try {
            Files.writeString(path, editor.getText(), StandardCharsets.UTF_8);
            currentFile = path;
            changed = false;
            statusLabel.setText("Saved " + path.getFileName());
            setTitle("Nova Notepad - " + path.getFileName());
        } catch (IOException e) {
            showError("Could not save file.");
        }
    }

    private boolean confirmDiscard() {
        if (!changed) return true;

        int choice = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Continue?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return choice == JOptionPane.YES_OPTION;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Nova Notepad", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NotepadApp().setVisible(true));
    }
}
