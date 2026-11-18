package dev.aether.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), WORLD("World"), CLIENT("Client");
    public final String label;
    Category(String l) { label = l; }
}
