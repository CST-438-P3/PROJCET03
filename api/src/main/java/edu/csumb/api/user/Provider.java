package edu.csumb.api.user;

/** Where an account signs in. The label is the name shown to users. */
public enum Provider {
    GITHUB("GitHub"),
    GOOGLE("Google");

    private final String label;

    Provider(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
