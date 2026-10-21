package one.empty3.corps3;

import one.empty3.library.*;
import one.empty3.library.core.physics.*;
import one.empty3.libs.Color;
import one.empty3.libs.Image;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class Problem extends JFrame {
    int insGraph = 0;
    private static final int N = 3;
    private static final double D = 200.0;
    private static final double V = 0.5;
    private double dt = 0.1;
    private final Scene scene;
    private JPanel simulationPanel;
    private JTabbedPane tabbedPane;
    private JTextArea jsonTextArea;
    private JList<Bille> billeJList;
    private DefaultListModel<Bille> listModel;
    private volatile boolean running = true;
    private volatile boolean step = true;
    private double time = 0.0;
    private Point3D min, max;
    private Force2 force;
    private Camera camera;
    private ZBufferImpl zBuffer;
    private long timeMillis = 0;
    private JTextField tField;
    private JTextField dtField;
    private volatile java.awt.image.BufferedImage currentImage;
    private static final int MAX_TRAIL_LENGTH = 200;
    private final List<List<Point3D>> trails = new ArrayList<>();
    private final Object simLock = new Object();

    static class BillePojo {
        Double mass;
        Double masse;
        double[] position;
        double[] vitesse;
        int[] color;
    }

    public Problem() {
        this.simulationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                java.awt.image.BufferedImage img = currentImage;
                if (img != null) {
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        this.listModel = new DefaultListModel<>();
        this.tabbedPane = new JTabbedPane();

        JPanel controlPanel = new JPanel();
        JButton playPauseBtn = new JButton(running ? "Pause" : "Start");
        playPauseBtn.addActionListener(e -> {
            running = !running;
            playPauseBtn.setText(running ? "Pause" : "Start");
            System.out.println("Status: " + (running ? "Started" : "Paused"));
        });
        JButton stepBtn = new JButton("Step");
        stepBtn.addActionListener(e -> {
            step = true;
            System.out.println("Status: Step");
        });

        controlPanel.add(new JLabel("Preset:"));
        JComboBox<String> presetCombo = new JComboBox<>(new String[] {
            "Solar System (Kepler)",
            "Figure-8 Choreography",
            "Lagrange Triangle",
            "Chaotic 3-Body",
            "Random Cluster"
        });
        presetCombo.addActionListener(e -> {
            String selected = (String) presetCombo.getSelectedItem();
            if (selected != null) {
                loadPreset(selected);
            }
        });
        controlPanel.add(presetCombo);

        controlPanel.add(new JLabel("t:"));
        tField = new JTextField(String.valueOf(time), 10);
        tField.addActionListener(e -> {
            try {
                time = Double.parseDouble(tField.getText());
                if (force != null) {
                    ((Force2) force).setT(time);
                }
            } catch (NumberFormatException ex) {
                tField.setText(String.valueOf(force instanceof Force2 ? ((Force2) force).getT() : time));
            }
        });
        controlPanel.add(tField);

        controlPanel.add(new JLabel("dt:"));
        dtField = new JTextField(String.valueOf(dt), 5);
        dtField.addActionListener(e -> {
            try {
                dt = Double.parseDouble(dtField.getText());
                if (force != null) {
                    force.setDt(dt);
                }
            } catch (NumberFormatException ex) {
                dtField.setText(String.valueOf(dt));
            }
        });
        controlPanel.add(dtField);

        controlPanel.add(playPauseBtn);
        controlPanel.add(stepBtn);

        JButton clearBtn = new JButton("Clear Image");
        clearBtn.addActionListener(e -> {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1000, 1000, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.dispose();
            currentImage = img;
            simulationPanel.repaint();
        });
        controlPanel.add(clearBtn);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(controlPanel, BorderLayout.NORTH);
        contentPanel.add(simulationPanel, BorderLayout.CENTER);

        this.tabbedPane.addTab("Actual content", contentPanel);
        this.tabbedPane.addTab("Editing of Bille objects", initEditorTab());

        setContentPane(tabbedPane);
        setSize(1000, 1000);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 1000));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        force = new Force2(time);
        force.amortissement = 0;
        force.intensiteRepulsion = 0;

        scene = new Scene();
        zBuffer = new ZBufferImpl(1000, 1000);
        zBuffer.setDisplayType(ZBufferImpl.DISPLAY_ALL);
        zBuffer.scene(scene);
        min = new Point3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        max = new Point3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        loadPreset("Solar System (Kepler)");

        setVisible(true);

        new Thread(this::simulationLoop).start();

        pack();
    }

    public void loadPreset(String name) {
        ArrayList<Bille> billes = new ArrayList<>();
        double defaultDt = 0.05;

        if ("Figure-8 Choreography".equals(name)) {
            defaultDt = 0.02;
            double m = 100.0;
            double scale = 200.0;
            double vScale = Math.sqrt(1.0 * m / scale);

            double x1 = -0.97000436 * scale;
            double y1 = 0.24308753 * scale;
            double vx1 = 0.466203685 * vScale;
            double vy1 = 0.43236573 * vScale;

            Bille b1 = new Bille();
            b1.masse = m;
            b1.position = new Point3D(x1, y1, 0.0);
            b1.vitesse = new Point3D(vx1, vy1, 0.0);
            b1.color = new Color(new java.awt.Color(255, 80, 80));
            billes.add(b1);

            Bille b2 = new Bille();
            b2.masse = m;
            b2.position = new Point3D(-x1, -y1, 0.0);
            b2.vitesse = new Point3D(vx1, vy1, 0.0);
            b2.color = new Color(new java.awt.Color(80, 150, 255));
            billes.add(b2);

            Bille b3 = new Bille();
            b3.masse = m;
            b3.position = new Point3D(0.0, 0.0, 0.0);
            b3.vitesse = new Point3D(-2.0 * vx1, -2.0 * vy1, 0.0);
            b3.color = new Color(new java.awt.Color(80, 255, 120));
            billes.add(b3);

        } else if ("Lagrange Triangle".equals(name)) {
            defaultDt = 0.05;
            double m = 100.0;
            double r = 200.0;
            double v = Math.sqrt(100.0 / (Math.sqrt(3.0) * r));

            java.awt.Color[] colors = new java.awt.Color[] {
                new java.awt.Color(255, 215, 0),
                new java.awt.Color(0, 220, 255),
                new java.awt.Color(255, 105, 180)
            };

            for (int i = 0; i < 3; i++) {
                double angle = i * 2.0 * Math.PI / 3.0;
                Bille b = new Bille();
                b.masse = m;
                b.position = new Point3D(r * Math.cos(angle), r * Math.sin(angle), 0.0);
                b.vitesse = new Point3D(-v * Math.sin(angle), v * Math.cos(angle), 0.0);
                b.color = new Color(colors[i]);
                billes.add(b);
            }

        } else if ("Chaotic 3-Body".equals(name)) {
            defaultDt = 0.05;
            Bille b1 = new Bille();
            b1.masse = 150.0;
            b1.position = new Point3D(-150.0, 50.0, 0.0);
            b1.vitesse = new Point3D(-0.3, 0.5, 0.1);
            b1.color = new Color(new java.awt.Color(255, 90, 90));
            billes.add(b1);

            Bille b2 = new Bille();
            b2.masse = 120.0;
            b2.position = new Point3D(150.0, -50.0, 0.0);
            b2.vitesse = new Point3D(0.4, -0.3, -0.1);
            b2.color = new Color(new java.awt.Color(90, 150, 255));
            billes.add(b2);

            Bille b3 = new Bille();
            b3.masse = 100.0;
            b3.position = new Point3D(0.0, 180.0, 40.0);
            b3.vitesse = new Point3D(-0.2, -0.3, 0.2);
            b3.color = new Color(new java.awt.Color(255, 215, 0));
            billes.add(b3);

        } else if ("Random Cluster".equals(name)) {
            defaultDt = 0.1;
            for (int i = 0; i < 4; i++) {
                Bille bille = new Bille();
                bille.masse = 80.0 + Math.random() * 80.0;
                bille.position = Point3D.random(200.0);
                bille.vitesse = Point3D.random(0.8);
                bille.color = new Color(new java.awt.Color((int) (Math.random() * 255), (int) (Math.random() * 255),
                        (int) (Math.random() * 255)));
                billes.add(bille);
            }

        } else {
            // Default: Solar System (Kepler)
            defaultDt = 0.05;
            // Central Star
            Bille star = new Bille();
            star.masse = 2000.0;
            star.position = new Point3D(0.0, 0.0, 0.0);
            star.vitesse = new Point3D(0.0, 0.0, 0.0);
            star.color = new Color(new java.awt.Color(255, 200, 0));
            billes.add(star);

            // Planet 1 (Inner)
            double r1 = 150.0;
            double v1 = Math.sqrt(2000.0 / r1);
            Bille p1 = new Bille();
            p1.masse = 5.0;
            p1.position = new Point3D(r1, 0.0, 0.0);
            p1.vitesse = new Point3D(0.0, v1, 0.0);
            p1.color = new Color(new java.awt.Color(0, 220, 255));
            billes.add(p1);

            // Planet 2 (Middle)
            double r2 = 270.0;
            double v2 = Math.sqrt(2000.0 / r2);
            Bille p2 = new Bille();
            p2.masse = 12.0;
            p2.position = new Point3D(0.0, r2, 0.0);
            p2.vitesse = new Point3D(-v2, 0.0, 0.0);
            p2.color = new Color(new java.awt.Color(100, 255, 120));
            billes.add(p2);

            // Planet 3 (Outer)
            double r3 = 400.0;
            double v3 = Math.sqrt(2000.0 / r3);
            Bille p3 = new Bille();
            p3.masse = 8.0;
            p3.position = new Point3D(-r3, 0.0, 0.0);
            p3.vitesse = new Point3D(0.0, -v3, 0.0);
            p3.color = new Color(new java.awt.Color(220, 120, 255));
            billes.add(p3);
        }

        this.dt = defaultDt;
        if (dtField != null) {
            SwingUtilities.invokeLater(() -> dtField.setText(String.valueOf(this.dt)));
        }
        synchronized (simLock) {
            listModel.clear();
            for (Bille b : billes) {
                listModel.addElement(b);
            }
            if (force != null) {
                force.configurer(billes);
                force.setDt(dt);
            }
        }
        updateJsonFromList();
        refreshSimulationView(true, true);
    }

    private JPanel initEditorTab() {
        JPanel panel = new JPanel(new BorderLayout());
        billeJList = new JList<>(listModel);
        billeJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bille) {
                    Bille b = (Bille) value;
                    setText("Bille (m=" + b.masse + ", p=" + b.position + ")");
                }
                return this;
            }
        });

        jsonTextArea = new JTextArea();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(billeJList),
                new JScrollPane(jsonTextArea));
        splitPane.setDividerLocation(300);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        JButton loadBtn = new JButton("Load");
        JButton updateDataBtn = new JButton("Update Data");
        JButton updateFromTextBtn = new JButton("Update from text");
        JButton addBtn = new JButton("Add");
        JButton removeBtn = new JButton("Remove");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(updateDataBtn);
        buttonPanel.add(updateFromTextBtn);
        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        saveBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter out = new PrintWriter(fileChooser.getSelectedFile())) {
                    out.println(jsonTextArea.getText());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        loadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (Scanner scanner = new Scanner(fileChooser.getSelectedFile())) {
                    StringBuilder sb = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine()).append("\n");
                    }
                    jsonTextArea.setText(sb.toString());
                    updateListFromJson();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        updateDataBtn.addActionListener(e -> updateJsonFromData());
        updateFromTextBtn.addActionListener(e -> updateListFromJson());

        addBtn.addActionListener(e -> {
            Bille b = editBilleDialog(null);
            if (b != null) {
                synchronized (simLock) {
                    force.getCourant().add(b);
                    listModel.addElement(b);
                }
                updateJsonFromList();
                refreshSimulationView(true, false);
            }
        });

        editBtn.addActionListener(e -> {
            int index = billeJList.getSelectedIndex();
            if (index != -1) {
                Bille selected = listModel.getElementAt(index);
                Bille edited = editBilleDialog(selected);
                if (edited != null) {
                    synchronized (simLock) {
                        selected.masse = edited.masse;
                        selected.position = edited.position;
                        selected.vitesse = edited.vitesse;
                        selected.color = edited.color;
                    }
                    billeJList.repaint();
                    updateJsonFromList();
                    refreshSimulationView(true, false);
                }
            }
        });

        ActionListener deleteAction = e -> {
            int index = billeJList.getSelectedIndex();
            if (index != -1) {
                synchronized (simLock) {
                    force.getCourant().remove(index);
                    listModel.remove(index);
                }
                updateJsonFromList();
                refreshSimulationView(true, false);
            }
        };
        removeBtn.addActionListener(deleteAction);
        deleteBtn.addActionListener(deleteAction);

        return panel;
    }

    private void updateJsonFromData() {
        updateJsonFromList();
        refreshSimulationView(false, false);
    }

    private Bille editBilleDialog(Bille b) {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField massField = new JTextField(b == null ? "1.0" : "" + b.masse);
        JTextField px = new JTextField(b == null ? "0" : "" + b.position.get(0));
        JTextField py = new JTextField(b == null ? "0" : "" + b.position.get(1));
        JTextField pz = new JTextField(b == null ? "0" : "" + b.position.get(2));
        JTextField vx = new JTextField(b == null ? "0" : "" + b.vitesse.get(0));
        JTextField vy = new JTextField(b == null ? "0" : "" + b.vitesse.get(1));
        JTextField vz = new JTextField(b == null ? "0" : "" + b.vitesse.get(2));

        panel.add(new JLabel("Mass:"));
        panel.add(massField);
        panel.add(new JLabel("Pos X:"));
        panel.add(px);
        panel.add(new JLabel("Pos Y:"));
        panel.add(py);
        panel.add(new JLabel("Pos Z:"));
        panel.add(pz);
        panel.add(new JLabel("Vit X:"));
        panel.add(vx);
        panel.add(new JLabel("Vit Y:"));
        panel.add(vy);
        panel.add(new JLabel("Vit Z:"));
        panel.add(vz);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Bille", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Bille res = new Bille();
                res.masse = Double.parseDouble(massField.getText());
                res.position = new Point3D(Double.parseDouble(px.getText()), Double.parseDouble(py.getText()),
                        Double.parseDouble(pz.getText()));
                res.vitesse = new Point3D(Double.parseDouble(vx.getText()), Double.parseDouble(vy.getText()),
                        Double.parseDouble(vz.getText()));
                res.color = b != null ? b.color : new Color(java.awt.Color.WHITE);
                return res;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input");
            }
        }
        return null;
    }

    private void updateJsonFromList() {
        List<BillePojo> pojos = new ArrayList<>();
        List<Bille> current;
        synchronized (simLock) {
            current = new ArrayList<>(force.getCourant());
        }
        for (Bille b : current) {
            BillePojo p = new BillePojo();
            p.mass = b.masse;
            p.position = new double[] { b.position.get(0), b.position.get(1), b.position.get(2) };
            p.vitesse = new double[] { b.vitesse.get(0), b.vitesse.get(1), b.vitesse.get(2) };
            try {
                java.awt.Color awt = b.color;
                p.color = new int[] { awt.getRed(), awt.getGreen(), awt.getBlue() };
            } catch (Exception e) {
                p.color = new int[] { 255, 255, 255 };
            }
            pojos.add(p);
        }
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(pojos);
        SwingUtilities.invokeLater(() -> jsonTextArea.setText(json));
    }

    private void updateListFromJson() {
        try {
            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<ArrayList<BillePojo>>() {
            }.getType();
            List<BillePojo> pojos = gson.fromJson(jsonTextArea.getText(), listType);
            // Synchronizes simulation state with parsed object list
            if (pojos != null) {
                if (pojos.isEmpty()) {
                    throw new IllegalArgumentException("JSON contains no celestial body definitions.");
                }
                // Maps JSON data to validated simulation objects
                ArrayList<Bille> newBilles = new ArrayList<>();
                for (int i = 0; i < pojos.size(); i++) {
                    BillePojo p = pojos.get(i);
                    if (p.position == null || p.position.length < 3) {
                        throw new IllegalArgumentException("Body #" + (i + 1) + " is missing a valid 3D position [x, y, z].");
                    }
                    Bille b = new Bille();
                    if (p.mass != null) {
                        b.masse = p.mass;
                    } else if (p.masse != null) {
                        b.masse = p.masse;
                    } else {
                        b.masse = 100.0;
                    }
                    b.position = new Point3D(p.position[0], p.position[1], p.position[2]);
                    if (p.vitesse != null && p.vitesse.length >= 3) {
                        b.vitesse = new Point3D(p.vitesse[0], p.vitesse[1], p.vitesse[2]);
                    } else {
                        b.vitesse = Point3D.O0;
                    }
                    if (p.color != null && p.color.length >= 3) {
                        // Clamps RGB values to valid color range
                        b.color = new Color(new java.awt.Color(
                                Math.max(0, Math.min(255, p.color[0])),
                                Math.max(0, Math.min(255, p.color[1])),
                                Math.max(0, Math.min(255, p.color[2]))
                        ));
                    } else {
                        b.color = new Color(java.awt.Color.WHITE);
                    }
                    newBilles.add(b);
                }
                synchronized (simLock) {
                    listModel.clear();
                    for (Bille b : newBilles) {
                        listModel.addElement(b);
                    }
                    force.configurer(newBilles);
                }
                refreshSimulationView(true, true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error parsing JSON: " + e.getMessage());
        }
    }

    public void refreshSimulationView(boolean resetTrails, boolean resetTime) {
        Image graph;
        synchronized (simLock) {
            if (resetTrails) {
                trails.clear();
            }
            // Resets simulation time and updates UI components
            if (resetTime) {
                time = 0.0;
                if (force instanceof Force2) {
                    ((Force2) force).setT(0.0);
                }
                if (tField != null) {
                    SwingUtilities.invokeLater(() -> tField.setText(String.format(Locale.US, "%.2f", 0.0)));
                }
            }
            List<Bille> current = force.getCourant();
            if (current == null || current.isEmpty()) {
                currentImage = null;
                SwingUtilities.invokeLater(() -> simulationPanel.repaint());
                return;
            }
            graph = graph(current);
        }
        if (graph != null) {
            currentImage = graph.getBi();
        }
        SwingUtilities.invokeLater(() -> simulationPanel.repaint());
    }

    private void simulationLoop() {
        while (true) {
            if (running || step) {
                Image graph;
                synchronized (simLock) {
                    force.calculer();
                    List<Bille> next = force.getCourant();
                    graph = graph(next);
                }

                if (graph != null) {
                    currentImage = graph.getBi();
                }

                if (step) {
                    step = false;
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void camera() {
        min = new Point3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        max = new Point3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        List<Bille> courant = force.getCourant();
        if (courant == null || courant.isEmpty()) {
            camera = new Camera(new Point3D(0.0, 0.0, 2000.0), Point3D.O0, Point3D.Y);
            return;
        }

        // Include current positions of all Billes
        courant.forEach(bille -> {
            for (int i = 0; i < 3; i++) {
                if (bille.position.get(i) < min.get(i)) {
                    min.set(i, bille.position.get(i));
                }
                if (bille.position.get(i) > max.get(i)) {
                    max.set(i, bille.position.get(i));
                }
            }
        });

        // Include all past trail coordinates to prevent trails from going off-screen
        for (List<Point3D> trail : trails) {
            for (Point3D p : trail) {
                for (int i = 0; i < 3; i++) {
                    if (p.get(i) < min.get(i)) {
                        min.set(i, p.get(i));
                    }
                    if (p.get(i) > max.get(i)) {
                        max.set(i, p.get(i));
                    }
                }
            }
        }

        Point3D center = min.plus(max).mult(0.5);
        Point3D diff = max.moins(min);
        double dist = diff.norme();
        if (dist < 100.0) {
            dist = 100.0;
        }

        Point3D eye = center.plus(new Point3D(0.0, 0.0, -dist * 2.0));
        camera = new Camera(eye, center, Point3D.Y);
    }

    private Image graph(List<Bille> next) {
        if(insGraph>0)
            return null;
        insGraph++;
        scene.cameras().clear();
        scene.clear();

        // Update trails
        while (trails.size() < next.size()) {
            trails.add(new ArrayList<>());
        }
        while (trails.size() > next.size()) {
            trails.remove(trails.size() - 1);
        }
        for (int i = 0; i < next.size(); i++) {
            Bille bille = next.get(i);
            List<Point3D> trail = trails.get(i);
            trail.add(new Point3D(bille.position));
            if (trail.size() > MAX_TRAIL_LENGTH) {
                trail.remove(0);
            }
        }

        // Render trajectory trail segments
        for (int i = 0; i < next.size(); i++) {
            Bille bille = next.get(i);
            List<Point3D> trail = trails.get(i);
            ColorTexture trailTexture = new ColorTexture(bille.color);
            for (int j = 0; j < trail.size() - 1; j++) {
                LineSegment seg = new LineSegment(trail.get(j), trail.get(j + 1));
                seg.texture(trailTexture);
                scene.add(seg);
            }
        }

        // Render each body as a 3D Sphere
        for (int i = 0; i < next.size(); i++) {
            Bille bille = next.get(i);

            double radius = Math.max(10.0, Math.min(100.0, 25.0 * Math.cbrt(Math.max(0.1, bille.masse))));
            Sphere sphere = new Sphere(bille.position, radius);
            sphere.texture(new ColorTexture(bille.color));
            scene.add(sphere);
        }

        camera();

        scene.cameraActive(camera);
        zBuffer.scene(scene);
        zBuffer.camera(camera);
        zBuffer.draw(scene);


        // Schedules UI refresh and updates time display asynchronously
        SwingUtilities.invokeLater(() -> {
            simulationPanel.repaint();
            if (tField != null && force != null) {
                tField.setText(String.format(Locale.US, "%.2f", ((Force2) force).getT()));
            }
        });

        insGraph--;

        //System.out.println("insGraph: " + insGraph);

        return zBuffer.image();
    }

    public static void main(String[] args) {
        new Problem();
    }
}