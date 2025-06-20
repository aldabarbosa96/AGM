package com.agm.screens;

import com.agm.model.Person;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class NodeView {
    private final Person person;
    private float x, y;
    public static final float RADIUS = 75f;

    public NodeView(Person person, float x, float y) {
        this.person = person;
        this.x = x;
        this.y = y;
    }

    public Person getPerson() {
        return person;
    }
    public float getX() { return x; }
    public float getY() { return y; }

    /** Dibuja el nodo; si está seleccionado dibuja un anillo exterior */
    public void draw(ShapeRenderer sr, boolean selected) {
        if (selected) {
            sr.setColor(Color.YELLOW);
            sr.circle(x, y, RADIUS + 6);
        }
        sr.setColor(Color.FIREBRICK);
        sr.circle(x, y, RADIUS);
    }

    /** True si el punto (px,py) está dentro del radio */
    public boolean contains(float px, float py) {
        float dx = px - x, dy = py - y;
        return dx*dx + dy*dy <= RADIUS*RADIUS;
    }
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
