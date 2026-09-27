package com.watabou.noosa;
import java.util.ArrayList;
public class Group extends Gizmo {
    protected ArrayList<Gizmo> members = new ArrayList<>();
    public int length;
    //mirror the real Group.add: it reparents the gizmo, which is what makes
    //Gizmo.parent a usable "was this really added?" probe.
    public synchronized Gizmo add(Gizmo g) {
        //faithful to the real Group.add: a null gizmo is a bug, not a no-op
        if (g.parent == this) return g;
        if (g.parent != null) g.parent.remove(g);
        members.add(g);
        g.parent = this;
        length++;
        return g;
    }
    public synchronized Gizmo addToFront(Gizmo g) { members.add(0, g); g.parent = this; return g; }
    public synchronized Gizmo addToBack(Gizmo g) { members.add(g); g.parent = this; return g; }
    public synchronized Gizmo remove(Gizmo g) { members.remove(g); if (g.parent == this) g.parent = null; return g; }
    public synchronized void clear() { for (Gizmo g : members) if (g.parent == this) g.parent = null; members.clear(); }
}
