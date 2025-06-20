// src/com/agm/model/Relation.java
package com.agm.model;

public class Relation {
    private final String parentId;
    private final String childId;

    public Relation(String parentId, String childId) {
        this.parentId = parentId;
        this.childId = childId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getChildId() {
        return childId;
    }
}
