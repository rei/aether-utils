package com.rei.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class ConfiguredAetherTest {

    private static final String REPO_URL = "https://repo-domain-url.com/public";
    public static final String REPO_USERNAME = "repo-username";
    public static final String REPO_PASSWORD = "test-password";

    @Test
    void builderWithoutCredentials_createsRepositoryWithoutAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setTempLocalRepo()
                .build();

        assertEquals(1, aether.getConfiguredRepositories().size());
        RemoteRepository repo = aether.getConfiguredRepositories().get(0);
        assertEquals(REPO_URL, repo.getUrl());
        assertNull(repo.getAuthentication());
    }

    @Test
    void builderWithoutCredentials_doesNotEnablePreemptiveAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setTempLocalRepo()
                .build();

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();
        Object preemptiveAuth = session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH);

        assertTrue(preemptiveAuth == null || Boolean.FALSE.equals(preemptiveAuth));
    }

    @Test
    void builderWithCredentials_createsRepositoryWithAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL, REPO_USERNAME, REPO_PASSWORD)
                .setTempLocalRepo()
                .build();

        assertEquals(1, aether.getConfiguredRepositories().size());
        RemoteRepository repo = aether.getConfiguredRepositories().get(0);
        assertEquals(REPO_URL, repo.getUrl());
        assertNotNull(repo.getAuthentication());
    }

    @Test
    void builderWithCredentials_enablesPreemptiveAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL, REPO_USERNAME, REPO_PASSWORD)
                .setTempLocalRepo()
                .build();

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();

        assertEquals(Boolean.TRUE, session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH));
    }

    @Test
    void addRemoteRepoWithCredentials_appliesToNamedRepo() {
        Aether aether = Aether.builder()
                .addRemoteRepo("default", REPO_URL, REPO_USERNAME, REPO_PASSWORD)
                .setTempLocalRepo()
                .build();

        assertEquals(1, aether.getConfiguredRepositories().size());
        assertNotNull(aether.getConfiguredRepositories().get(0).getAuthentication());
    }
}
