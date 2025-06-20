// TreeInputController.java
package com.agm.ui;

import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.agm.screens.NodeView;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.List;
import java.util.function.Consumer;

public class TreeInputController {
    private final Stage stage;
    private final OrthographicCamera camera;
    private final List<NodeView> nodes;

    public TreeInputController(Stage stage,
                               OrthographicCamera cam,
                               List<NodeView> nodes) {
        this.stage = stage;
        this.camera = cam;
        this.nodes = nodes;
    }

    public void setup(Consumer<NodeView> onSelect) {
        GestureDetector gesture = new GestureDetector(new PanZoomGesture(camera));
        InputAdapter touch = new NodeTouchListener(stage, camera, nodes, onSelect);
        InputMultiplexer mux = new InputMultiplexer(stage, gesture, touch);
        Gdx.input.setInputProcessor(mux);
    }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, false);
    }
}
