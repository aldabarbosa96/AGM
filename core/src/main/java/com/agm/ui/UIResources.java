package com.agm.ui;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;

public final class UIResources {

    private static Skin skin;

    public static Skin skin() {
        if (skin != null) return skin;

        skin = new Skin();

        // ---------- 1 · Fuente ----------
        BitmapFont font = new BitmapFont();              // default.fnt embebido
        skin.add("default-font", font);

        // ---------- 2 · “white” 1×1 ----------
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture whiteTex = new Texture(pm);
        pm.dispose();                                   // ¡liberamos memoria nativa!
        skin.add("white", whiteTex);                    // ← registrar como Texture

        // ---------- 3 · Label ----------
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        // ---------- 4 · Window / Dialog ----------
        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = font;
        ws.titleFontColor = Color.WHITE;
        ws.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", ws);

        // ---------- 5 · Botón ----------
        TextButton.TextButtonStyle bs = new TextButton.TextButtonStyle();
        bs.font  = font;
        bs.up    = skin.newDrawable("white", Color.DARK_GRAY);
        bs.down  = skin.newDrawable("white", Color.GRAY);
        bs.over  = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", bs);

        // --- 6 · Botón “grande” reutilizando el estilo anterior ---
        BitmapFont bigFont = new BitmapFont();
        bigFont.getData().setScale(1.5f);
        skin.add("big-font", bigFont);

        TextButton.TextButtonStyle bigBtn = new TextButton.TextButtonStyle(bs); // ctor-copia
        bigBtn.font = bigFont;
        skin.add("big", bigBtn);

        return skin;
    }

    /** Clase utilitaria: impedimos instanciación */
    private UIResources() {}
}
