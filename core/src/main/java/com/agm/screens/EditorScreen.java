package com.agm.screens;

import com.agm.MainGame;
import com.agm.model.FamilyTree;
import com.agm.model.Person;
import com.agm.model.Relation;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

public class EditorScreen extends AbstractScreen {
    private enum Mode {VIEW, ADD_CHILD, ADD_PARENT}

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private Skin skin;
    private BitmapFont font;
    private final List<NodeView> nodes = new ArrayList<>();
    private final FamilyTree tree = new FamilyTree();
    private NodeView selectedNode = null;
    private Table menuTable = null;

    public EditorScreen(MainGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        // Skin y fuente
        skin = createBasicSkin();
        font = new BitmapFont();
        font.getData().setScale(2f);
        skin.add("big-font", font);

        // Cámara con zoom inicial
        OrthographicCamera cam = (OrthographicCamera) stage.getCamera();
        cam.zoom = 0.7f;
        cam.update();

        // Carga existente o crea raíz
        loadTree();
        if (tree.getPeople().isEmpty()) {
            Person root = new Person(UUID.randomUUID().toString(),
                "Raíz", "", LocalDate.now(), null, "");
            tree.addPerson(root);
            nodes.add(new NodeView(root, 0, 0));
        }
        // Posiciona todos los nodos
        layoutTree();

        // Configura todos los InputProcessors EN ORDEN
        GestureDetector gestureDetector = new GestureDetector(new GestureAdapter() {
            private float startZoom = cam.zoom, lastDist = -1f;
            @Override public boolean pan(float x, float y, float dx, float dy) {
                cam.position.add(-dx * cam.zoom, dy * cam.zoom, 0);
                cam.update();
                return true;
            }
            @Override public boolean zoom(float initialDist, float distance) {
                if (initialDist != lastDist) {
                    lastDist = initialDist;
                    startZoom = cam.zoom;
                }
                cam.zoom = MathUtils.clamp(startZoom * (initialDist/distance), 0.3f, 3f);
                cam.update();
                return true;
            }
            @Override public void pinchStop() { lastDist = -1f; }
        });

        InputAdapter touchSelect = new InputAdapter() {
            @Override public boolean touchDown(int sx, int sy, int p, int b) {
                Vector3 wp = new Vector3(sx, sy, 0);
                stage.getViewport().unproject(wp);
                NodeView hit = findHitNode(wp.x, wp.y);
                if (hit != null) {
                    selectedNode = hit;
                    showNodeMenu(hit);
                } else if (menuTable != null) {
                    menuTable.remove();
                    menuTable = null;
                    selectedNode = null;
                }
                return true;
            }
        };

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(stage);
        mux.addProcessor(gestureDetector);
        mux.addProcessor(touchSelect);
        Gdx.input.setInputProcessor(mux);
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        batch.setProjectionMatrix(stage.getCamera().combined);

        float nameOffset = 30f;

        // Dibujar conexiones
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Map<String, List<NodeView>> groups = new HashMap<>();
        for (Relation rel : tree.getRelations()) {
            NodeView child = findNodeById(rel.getChildId());
            if (child != null) {
                groups.computeIfAbsent(rel.getParentId(), k -> new ArrayList<>()).add(child);
            }
        }
        for (Map.Entry<String, List<NodeView>> e : groups.entrySet()) {
            NodeView parent = findNodeById(e.getKey());
            List<NodeView> children = e.getValue();
            if (parent == null || children.isEmpty()) continue;

            float px = parent.getX();
            float py = parent.getY();
            float connectorY = py - NodeView.RADIUS - 20;

            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.line(px, py - NodeView.RADIUS, px, connectorY);

            float firstX = children.get(0).getX();
            float lastX = children.get(children.size() - 1).getX();
            shapeRenderer.line(firstX, connectorY, lastX, connectorY);

            for (NodeView c : children) {
                float stopY = c.getY() + NodeView.RADIUS + nameOffset;
                shapeRenderer.line(c.getX(), connectorY, c.getX(), stopY);
            }
        }
        shapeRenderer.end();

        // Dibujar nodos
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (NodeView nv : nodes) {
            nv.draw(shapeRenderer, nv == selectedNode);
        }
        shapeRenderer.end();

        // Dibujar nombres
        batch.begin();
        for (NodeView nv : nodes) {
            String name = nv.getPerson().getFirstName();
            GlyphLayout layout = new GlyphLayout(font, name);
            float nameX = nv.getX() - layout.width / 2f;
            float nameY = nv.getY() + NodeView.RADIUS + nameOffset;
            font.draw(batch, layout, nameX, nameY);
        }
        batch.end();

        super.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, false);
    }

    @Override
    public void dispose() {
        super.dispose();
        saveTree();
        shapeRenderer.dispose();
        batch.dispose();
        skin.dispose();
        font.dispose();
    }

    private void showNodeMenu(NodeView node) {
        if (menuTable != null) menuTable.remove();

        menuTable = new Table(skin);
        menuTable.setBackground("white");
        menuTable.pad(20).defaults().pad(10).minWidth(180).minHeight(50);

        TextButton btnView = new TextButton("Ver info", skin, "big");
        TextButton btnEdit = new TextButton("Editar nodo", skin, "big");
        TextButton btnAddCh = new TextButton("Añadir Hijo", skin, "big");
        TextButton btnAddPar = new TextButton("Añadir Padre", skin, "big");

        menuTable.add(btnView).row();
        menuTable.add(btnEdit).row();
        menuTable.add(btnAddCh).row();
        menuTable.add(btnAddPar);

        btnView.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Person p = node.getPerson();
                Dialog info = new Dialog("Info de " + p.getFirstName(), skin);
                info.text("Nombre:     " + p.getFirstName() + " " + p.getLastName());
                info.row();
                info.text("Nacimiento: " + p.getBirthDate());
                info.row();
                info.text("Defunción:  " + (p.getDeathDate() != null ? p.getDeathDate() : "—"));
                info.row();
                info.text("Nota:       " + (p.getQuote().isEmpty() ? "—" : "\"" + p.getQuote() + "\""));
                info.button("Cerrar");
                info.show(stage);
            }
        });

        btnEdit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editNodeData(node, null);
                menuTable.remove();
                menuTable = null;
            }
        });

        btnAddCh.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editNodeData(node, newPerson -> {
                    tree.addPerson(newPerson);
                    tree.addRelation(node.getPerson().getId(), newPerson.getId());
                    nodes.add(new NodeView(newPerson, 0, 0));
                    layoutTree();
                });
                menuTable.remove();
                menuTable = null;
            }
        });

        btnAddPar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editNodeData(node, newPerson -> {
                    tree.addPerson(newPerson);
                    tree.addRelation(newPerson.getId(), node.getPerson().getId());
                    nodes.add(new NodeView(newPerson, 0, 0));
                    layoutTree();
                });
                menuTable.remove();
                menuTable = null;
            }
        });

        menuTable.pack();
        float px = node.getX() - menuTable.getWidth() / 2f;
        float py = node.getY() - NodeView.RADIUS - menuTable.getHeight() - 20;
        menuTable.setPosition(px, py);
        stage.addActor(menuTable);
    }

    private void editNodeData(NodeView base, Consumer<Person> onCreated) {
        String initFull = onCreated == null ? base.getPerson().getFirstName() + " " + base.getPerson().getLastName() : "";
        String initBD = onCreated == null ? base.getPerson().getBirthDate().toString() : LocalDate.now().toString();
        String initDD = onCreated == null && base.getPerson().getDeathDate() != null ? base.getPerson().getDeathDate().toString() : "";
        String initQ = onCreated == null ? base.getPerson().getQuote() : "";

        // 1. Nombre
        Gdx.input.getTextInput(new TextInputListener() {
            @Override
            public void input(String fullName) {
                String[] parts = fullName.trim().split("\\s+", 2);
                String fName = parts[0], lName = parts.length > 1 ? parts[1] : "";

                // 2. Nacimiento
                Gdx.input.getTextInput(new TextInputListener() {
                    @Override
                    public void input(String bdText) {
                        LocalDate bd;
                        try {
                            bd = LocalDate.parse(bdText);
                        } catch (Exception e) {
                            bd = LocalDate.now();
                        }

                        // 3. Defunción
                        LocalDate finalBd = bd;
                        Gdx.input.getTextInput(new TextInputListener() {
                            @Override
                            public void input(String ddText) {
                                LocalDate dd = null;
                                if (!ddText.trim().isEmpty()) {
                                    try {
                                        dd = LocalDate.parse(ddText);
                                    } catch (Exception e) {
                                    }
                                }

                                // 4. Cita
                                LocalDate finalDd = dd;
                                Gdx.input.getTextInput(new TextInputListener() {
                                    @Override
                                    public void input(String quoteText) {
                                        if (onCreated == null) {
                                            // editar
                                            base.getPerson().setFirstName(fName);
                                            base.getPerson().setLastName(lName);
                                            base.getPerson().setBirthDate(finalBd);
                                            base.getPerson().setDeathDate(finalDd);
                                            base.getPerson().setQuote(quoteText);
                                        } else {
                                            // crear
                                            Person np = new Person(UUID.randomUUID().toString(), fName, lName, finalBd, finalDd, quoteText);
                                            onCreated.accept(np);
                                        }
                                    }

                                    @Override
                                    public void canceled() {
                                    }
                                }, "Cita u Observación", initQ, "Texto libre");
                            }

                            @Override
                            public void canceled() {
                            }
                        }, "Fecha de defunción (YYYY-MM-DD)", initDD, "YYYY-MM-DD");
                    }

                    @Override
                    public void canceled() {
                    }
                }, "Fecha de nacimiento (YYYY-MM-DD)", initBD, "YYYY-MM-DD");
            }

            @Override
            public void canceled() {
            }
        }, onCreated == null ? "Editar Nombre" : "Nuevo Nombre", initFull, "Nombre Apellidos");
    }

    // ─── Métodos de búsqueda ───────────────────────────────────────────────────

    private NodeView findHitNode(float x, float y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).contains(x, y)) return nodes.get(i);
        }
        return null;
    }

    private NodeView findNodeById(String id) {
        return nodes.stream().filter(nv -> nv.getPerson().getId().equals(id)).findFirst().orElse(null);
    }

    // ─── Layout recursivo para evitar solapamientos ────────────────────────────

    private List<NodeView> getChildren(NodeView parent) {
        List<NodeView> result = new ArrayList<>();
        for (Relation r : tree.getRelations()) {
            if (r.getParentId().equals(parent.getPerson().getId())) {
                NodeView c = findNodeById(r.getChildId());
                if (c != null) result.add(c);
            }
        }
        return result;
    }

    private float computeSubtreeWidth(NodeView node, Map<String, Float> widthMap) {
        List<NodeView> children = getChildren(node);
        if (children.isEmpty()) {
            float w = NodeView.RADIUS * 2 + 50f;
            widthMap.put(node.getPerson().getId(), w);
            return w;
        }
        float total = 0f;
        for (NodeView c : children) {
            total += computeSubtreeWidth(c, widthMap);
        }
        widthMap.put(node.getPerson().getId(), total);
        return total;
    }

    private void layoutSubtree(NodeView node, float centerX, float y, Map<String, Float> widthMap) {
        node.setPosition(centerX, y);
        List<NodeView> children = getChildren(node);
        if (children.isEmpty()) return;

        float subtreeWidth = widthMap.get(node.getPerson().getId());
        float startX = centerX - subtreeWidth / 2f;
        float verticalGap = NodeView.RADIUS * 2 + 100f;

        for (NodeView c : children) {
            float childWidth = widthMap.get(c.getPerson().getId());
            float childCenter = startX + childWidth / 2f;
            layoutSubtree(c, childCenter, y - verticalGap, widthMap);
            startX += childWidth;
        }
    }

    private void layoutTree() {
        if (nodes.isEmpty()) return;
        NodeView root = nodes.get(0);
        float startX = stage.getViewport().getWorldWidth() / 2f;
        float startY = stage.getViewport().getWorldHeight() / 2f;

        Map<String, Float> widthMap = new HashMap<>();
        computeSubtreeWidth(root, widthMap);
        layoutSubtree(root, startX, startY, widthMap);
    }

    // ─── Skin básico ──────────────────────────────────────────────────────────

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        // Fuente por defecto
        BitmapFont f = new BitmapFont();
        f.getData().setScale(1.2f);
        skin.add("default-font", f);

        // Textura blanca
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        skin.add("white", new Texture(pix));
        pix.dispose();

        // Botones pequeños
        TextButton.TextButtonStyle small = new TextButton.TextButtonStyle();
        small.font = f;
        small.up = skin.newDrawable("white", Color.DARK_GRAY);
        small.down = skin.newDrawable("white", Color.GRAY);
        small.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", small);

        // Botones grandes
        BitmapFont bigF = new BitmapFont();
        bigF.getData().setScale(1.5f);
        skin.add("big-font", bigF);
        TextButton.TextButtonStyle big = new TextButton.TextButtonStyle();
        big.font = bigF;
        big.up = skin.newDrawable("white", Color.DARK_GRAY);
        big.down = skin.newDrawable("white", Color.GRAY);
        big.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("big", big);

        return skin;
    }

    /**
     * Guarda el árbol en familytree.json (local).
     */
    private void saveTree() {
        Json json = new Json();
        String data = json.toJson(tree);
        FileHandle fh = Gdx.files.local("familytree.json");
        fh.writeString(data, false);
    }

    /**
     * Carga el árbol si existe y reconstruye Person, Relation y NodeView.
     */
    private void loadTree() {
        FileHandle fh = Gdx.files.local("familytree.json");
        if (!fh.exists()) return;

        Json json = new Json();
        FamilyTree loaded = json.fromJson(FamilyTree.class, fh.readString());

        // 1) Vacío y relleno la lista interna de personas
        tree.getPeople().clear();
        tree.getPeople().putAll(loaded.getPeople());

        // 2) Vacío y relleno la lista interna de relaciones
        tree.getRelationsMutable().clear();
        tree.getRelationsMutable().addAll(loaded.getRelations());

        // 3) Reconstruyo las vistas de nodos
        nodes.clear();
        for (Person p : tree.getPeople().values()) {
            nodes.add(new NodeView(p, 0, 0));
        }
    }
}
