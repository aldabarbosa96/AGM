package com.agm;

import com.agm.screens.EditorScreen;
import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainGame extends Game {
    @Override
    public void create() {
        setScreen(new EditorScreen(this));
    }
}

