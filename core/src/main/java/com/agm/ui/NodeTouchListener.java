// NodeTouchListener.java
package com.agm.ui;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.agm.screens.NodeView;

import java.util.List;
import java.util.function.Consumer;

public class NodeTouchListener extends InputAdapter {
    private final Stage stage;
    private final OrthographicCamera cam;
    private final List<NodeView> nodes;
    private final Consumer<NodeView> callback;

    public NodeTouchListener(Stage stage, OrthographicCamera cam, List<NodeView> nodes, Consumer<NodeView> callback) {
        this.stage = stage;
        this.cam = cam;
        this.nodes = nodes;
        this.callback = callback;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        Vector3 wp = new Vector3(sx, sy, 0);
        stage.getViewport().unproject(wp);

        NodeView hit = null;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            NodeView nv = nodes.get(i);
            if (nv.contains(wp.x, wp.y)) {
                hit = nv;
                break;
            }
        }

        /* Notificamos siempre: si no hay nodo devolvemos null */
        callback.accept(hit);

        /* Solo consumimos el evento cuando realmente se pulsó un nodo */
        return hit != null;
    }

}
