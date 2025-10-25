package com.example.sivareats.ui.profile;

public class LocationItemActivity {
    private final String title;
    private final String address;
    private final boolean preferred;

    public LocationItemActivity(String title, String address, boolean preferred) {
        this.title = title;
        this.address = address;
        this.preferred = preferred;
    }

    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public boolean isPreferred() { return preferred; }

    public boolean matches(String q) {
        if (q == null || q.trim().isEmpty()) return true;
        String t = q.toLowerCase().trim();
        return title.toLowerCase().contains(t) || address.toLowerCase().contains(t);
    }
}
