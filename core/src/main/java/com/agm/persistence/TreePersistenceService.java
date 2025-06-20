// TreePersistenceService.java
package com.agm.persistence;

import com.agm.model.FamilyTree;
import com.agm.model.Person;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

import java.time.LocalDate;
import java.util.UUID;

public class TreePersistenceService {
    private static final String FILE = "familytree.json";
    private final FamilyTree tree;

    public TreePersistenceService(FamilyTree tree) {
        this.tree = tree;
    }

    public void loadOrCreateRoot() {
        FileHandle fh = Gdx.files.local(FILE);
        if (fh.exists()) {
            Json json = new Json();
            FamilyTree loaded = json.fromJson(FamilyTree.class, fh.readString());
            tree.getPeople().clear();
            tree.getPeople().putAll(loaded.getPeople());
            tree.getRelationsMutable().clear();
            tree.getRelationsMutable().addAll(loaded.getRelations());
        } else {
            Person root = new Person(UUID.randomUUID().toString(),
                "Raíz", "", LocalDate.now(), null, "");
            tree.addPerson(root);
        }
    }

    public void save() {
        Json json = new Json();
        String data = json.toJson(tree);
        Gdx.files.local(FILE).writeString(data, false);
    }
}
