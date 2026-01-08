package net.spookly.kodama.brain.security;

public enum Role {
    ADMIN,
    OPERATOR,
    VIEWER;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
