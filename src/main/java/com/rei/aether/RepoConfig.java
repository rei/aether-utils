package com.rei.aether;

class RepoConfig {
    final String url;
    final String username;
    final String password;

    RepoConfig(String url) {
        this(url, null, null);
    }

    RepoConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    boolean hasCredentials() {
        return username != null && !username.isEmpty() && password != null;
    }
}
