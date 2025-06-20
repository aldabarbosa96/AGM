package com.agm.model;

import java.util.Objects;

public class Relation {
    private final String fromId;
    private final String toId;
    private final RelationType type;

    public Relation(String fromId, String toId, RelationType type) {
        this.fromId = fromId;
        this.toId = toId;
        this.type = type;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public RelationType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation)) return false;
        Relation r = (Relation) o;
        return fromId.equals(r.fromId) && toId.equals(r.toId) && type == r.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, type);
    }
}
