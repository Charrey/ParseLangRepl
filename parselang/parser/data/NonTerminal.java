package parselang.parser.data;

public final class NonTerminal extends Node {

    public SpecialStatus getSpecialStatus() {
        return status;
    }

    @Override
    public Node copy() {
        return new NonTerminal(name, status);
    }

    public enum SpecialStatus {
        NONE, AUTOGENERATED_STAR
    }

    private final String name;

    public NonTerminal(String name) {
        this.name = name;
    }

    public NonTerminal(String name, SpecialStatus status) {
        this.name = name;
        this.status = status;
    }


    private SpecialStatus status = SpecialStatus.NONE;

    public void setSpecialStatus(SpecialStatus specialStatus){
        status = specialStatus;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public boolean equals(Object other) {
        if (!(other instanceof NonTerminal)) {
            return false;
        }
        return name.equals(((NonTerminal) other).getName());
    }

    public int hashCode() {
        return name.hashCode();
    }


}