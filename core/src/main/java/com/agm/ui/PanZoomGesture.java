package com.agm.ui;

import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class PanZoomGesture extends GestureAdapter {
    private final OrthographicCamera cam;
    private float startZoom, lastDist = -1f;

    public PanZoomGesture(OrthographicCamera cam) {
        this.cam = cam;
    }

    @Override
    public boolean pan(float x, float y, float dx, float dy) {
        cam.position.add(-dx * cam.zoom, dy * cam.zoom, 0);
        cam.update();
        return true;
    }

    @Override
    public boolean zoom(float initialDist, float distance) {
        if (initialDist != lastDist) {
            lastDist = initialDist;
            startZoom = cam.zoom;
        }
        cam.zoom = MathUtils.clamp(startZoom * (initialDist / distance), 0.3f, 3f);
        cam.update();
        return true;
    }

    @Override
    public void pinchStop() {
        lastDist = -1f;
    }
}
