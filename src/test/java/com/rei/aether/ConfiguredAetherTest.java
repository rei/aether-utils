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
    void builderWithoutAuth_createsRepositoryWithoutAuth() {
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
    void builderWithoutPreemptiveAuth_doesNotEnablePreemptiveAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setTempLocalRepo()
                .build();

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();
        Object preemptiveAuth = session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH);

        assertTrue(preemptiveAuth == null || Boolean.FALSE.equals(preemptiveAuth));
    }

    @Test
    void builderWithAuthentication_createsRepositoryWithAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setAuthentication(REPO_USERNAME, REPO_PASSWORD)
                .setTempLocalRepo()
                .build();

        assertEquals(1, aether.getConfiguredRepositories().size());
        RemoteRepository repo = aether.getConfiguredRepositories().get(0);
        assertEquals(REPO_URL, repo.getUrl());
        assertNotNull(repo.getAuthentication());
    }

    @Test
    void builderWithPreemptiveAuthentication_enablesPreemptiveAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setPreemptiveAuthentication(true)
                .setTempLocalRepo()
                .build();

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();

        assertEquals(true, session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH));
    }

    @Test
    void builderWithAuthenticationAndPreemptiveAuth_createsAuthenticatedRepoAndEnablesPreemptiveAuth() {
        Aether aether = Aether.builder()
                .setDefaultRemoteRepo(REPO_URL)
                .setAuthentication(REPO_USERNAME, REPO_PASSWORD)
                .setPreemptiveAuthentication(true)
                .setTempLocalRepo()
                .build();

        RemoteRepository repo = aether.getConfiguredRepositories().get(0);
        assertNotNull(repo.getAuthentication());

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();
        assertEquals(true, session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH));
    }
}
