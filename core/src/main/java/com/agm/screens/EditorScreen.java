package com.agm.screens;

import com.agm.MainGame;
import com.agm.model.FamilyTree;
import com.agm.model.Person;
import com.agm.persistence.TreePersistenceService;
import com.agm.ui.NodeMenuController;
import com.agm.ui.TreeInputController;
import com.agm.ui.TreeLayoutEngine;
import com.agm.ui.TreeRenderer;
import com.agm.ui.UIResources;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

import java.util.ArrayList;
import java.util.List;

public class EditorScreen extends AbstractScreen {

    /* ---------- Datos de escena ---------- */
    private final FamilyTree tree = new FamilyTree();
    private final List<NodeView> nodes = new ArrayList<>();

    /* Servicios/UI */
    private Skin skin;
    private TreePersistenceService persistence;
    private TreeLayoutEngine layoutEngine;
    private TreeRenderer renderer;
    private TreeInputController inputController;
    private NodeMenuController menuController;

    private NodeView selectedNode;

    public EditorScreen(MainGame game) {
        super(game);
    }

    /* ---------- Ciclo de vida ---------- */
    @Override
    public void show() {
        super.show();

        /* 1. Skin y cámara */
        skin = UIResources.skin();
        OrthographicCamera cam = (OrthographicCamera) stage.getCamera();
        cam.zoom = 0.7f;
        cam.update();

        /* 2. Persistencia */
        persistence = new TreePersistenceService(tree);
        persistence.loadOrCreateRoot();

        nodes.clear();
        for (Person p : tree.getPeople().values()) {
            nodes.add(new NodeView(p, 0, 0));
        }

        /* 3. Layout */
        layoutEngine = new TreeLayoutEngine(tree, nodes, stage);
        layoutEngine.layoutAll();

        /* 4. Render */
        renderer = new TreeRenderer(stage);

        /* 5. Input + menú */
        menuController = new NodeMenuController(stage, tree, nodes, skin,
            new Runnable() { @Override public void run() { layoutEngine.layoutAll(); } });

        inputController = new TreeInputController(stage, cam, nodes);
        inputController.setup(node -> {
            if (node != null) {            // clic sobre un nodo
                selectedNode = node;
                menuController.show(node);
            } else {                       // clic en zona vacía
                selectedNode = null;
                menuController.close();
            }
        });
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(nodes, tree.getRelations(), selectedNode);
        super.render(delta);
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        inputController.resize(w, h);
    }

    @Override
    public void dispose() {
        super.dispose();
        persistence.save();
        renderer.dispose();
        menuController.dispose();
        skin.dispose();
    }
}
