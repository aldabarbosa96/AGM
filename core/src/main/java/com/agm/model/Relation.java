package com.agm.model;

public class Relation {
    private final String fromId;
    private final String toId;
    private final RelationType type;

    public Relation(String fromId, String toId, RelationType type) {
        this.fromId = fromId;
        this.toId   = toId;
        this.type   = type;
    }

    public String getFromId()   { return fromId; }
    public String getToId()     { return toId;   }
    public RelationType getType(){ return type;  }
}

