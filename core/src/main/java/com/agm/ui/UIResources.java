package com.agm.ui;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;

public class UIResources {

    private static Skin skin;

    public static Skin skin() {
        if (skin != null) return skin;

        skin = new Skin();

        // ---------- 1 · Fuente ----------
        BitmapFont font = new BitmapFont();              //  default.fnt embebido
        skin.add("default-font", font);

        // ---------- 2 · “Blanco” 1×1 ----------
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        TextureRegionDrawable white = new TextureRegionDrawable(new Texture(pm));
        skin.add("white", white);

        // ---------- 3 · Label ----------
        Label.LabelStyle ls = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", ls);

        // ---------- 4 · Window / Dialog ----------
        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = font;
        ws.titleFontColor = Color.WHITE;
        ws.background = white.tint(Color.DARK_GRAY);
        skin.add("default", ws);

        // ---------- 5 · Botón ----------
        TextButton.TextButtonStyle bs = new TextButton.TextButtonStyle();
        bs.font = font;
        bs.up   = white.tint(Color.DARK_GRAY);
        bs.down = white.tint(Color.GRAY);
        bs.over = white.tint(Color.LIGHT_GRAY);
        skin.add("default", bs);

        // (Añade otros estilos cuando los necesites)

        return skin;
    }

    /** No instanciable */
    private UIResources() {}
}
