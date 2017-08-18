package br.ufsc.inf.lapesd.alignator.core;

public class Alignment {

    private String uri1;
    private String uri2;
    private double strength;
    private String relation;

    public String getUri1() {
        return uri1;
    }

    public void setUri1(String uri1) {
        this.uri1 = uri1;
    }

    public String getUri2() {
        return uri2;
    }

    public void setUri2(String uri2) {
        this.uri2 = uri2;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    @Override
    public String toString() {
        return "Alignment [uri1=" + uri1 + ", uri2=" + uri2 + ", strength=" + strength + ", relation=" + relation + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alignment alignment = (Alignment) o;

        if (Double.compare(alignment.strength, strength) != 0) return false;
        if (uri1 != null ? !uri1.equals(alignment.uri1) : alignment.uri1 != null) return false;
        if (uri2 != null ? !uri2.equals(alignment.uri2) : alignment.uri2 != null) return false;
        return relation != null ? relation.equals(alignment.relation) : alignment.relation == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = uri1 != null ? uri1.hashCode() : 0;
        result = 31 * result + (uri2 != null ? uri2.hashCode() : 0);
        temp = Double.doubleToLongBits(strength);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        return result;
    }
}
