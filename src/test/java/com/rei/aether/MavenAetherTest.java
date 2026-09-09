package com.rei.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

class MavenAetherTest {

    public static final String REPO_URL = "http://central";
    public static final String REPO_ID = "central";
    public static final String REPO_USERNAME = "user";
    public static final String REPO_PASSWORD = "pass";

    @Test
    void canResolveSingleDependency() {
        Aether aether = Aether.fromMavenSettings();
        aether.getConfiguredRepositories().forEach(System.out::println);
        Artifact artifact = aether.resolveSingleArtifact("org.junit.jupiter:junit-jupiter-api:RELEASE");
        assertNotNull(artifact);
    }

    @Test
    void canResolveDependencies() {
        Aether aether = Aether.fromMavenSettings();
        List<Artifact> dependencies = aether.resolveDependencies(
                new DefaultArtifact("org.junit.jupiter:junit-jupiter-api:5.9.3"));
        assertFalse(dependencies.isEmpty());
        dependencies.forEach(d -> {
            System.out.println(d);
            assertNotNull(d.getFile());
        });
    }

    @Test
    void attachesServerCredentialsToMatchingRepository() throws Exception {
        MavenAether aether = new MavenAether();
        Settings settings = new Settings();

        Profile profile = new Profile();
        profile.setId("dev");
        Repository repository = new Repository();
        repository.setId(REPO_ID);
        repository.setUrl(REPO_URL);
        profile.addRepository(repository);
        settings.addProfile(profile);
        settings.addActiveProfile("dev");

        Server server = new Server();
        server.setId(REPO_ID);
        server.setUsername(REPO_USERNAME);
        server.setPassword(REPO_PASSWORD);
        settings.addServer(server);

        setSettings(aether, settings);

        List<RemoteRepository> repos = aether.getConfiguredRepositories();
        assertEquals(1, repos.size());
        RemoteRepository repo = repos.get(0);
        assertEquals(REPO_ID, repo.getId());
        assertNotNull(repo.getAuthentication());
    }

    @Test
    void noServerCredentialsForRepository_withoutMatchingServer() throws Exception {
        MavenAether aether = new MavenAether();
        Settings settings = new Settings();

        Profile profile = new Profile();
        profile.setId("dev");
        Repository repository = new Repository();
        repository.setId(REPO_ID);
        repository.setUrl(REPO_URL);
        profile.addRepository(repository);
        settings.addProfile(profile);
        settings.addActiveProfile("dev");

        setSettings(aether, settings);

        List<RemoteRepository> repos = aether.getConfiguredRepositories();
        assertEquals(1, repos.size());
        assertNull(repos.get(0).getAuthentication());
    }

    @Test
    void enablesPreemptiveAuthWhenCredentialsPresent() throws Exception {
        MavenAether aether = new MavenAether();
        Settings settings = new Settings();

        Profile profile = new Profile();
        profile.setId("dev");
        Repository repository = new Repository();
        repository.setId(REPO_ID);
        repository.setUrl(REPO_URL);
        profile.addRepository(repository);
        settings.addProfile(profile);
        settings.addActiveProfile("dev");

        Server server = new Server();
        server.setId(REPO_ID);
        server.setUsername(REPO_USERNAME);
        server.setPassword(REPO_PASSWORD);
        settings.addServer(server);

        setSettings(aether, settings);

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();
        assertEquals(Boolean.TRUE, session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH));
    }

    @Test
    void doesNotEnablePreemptiveAuthWhenNoCredentials() throws Exception {
        MavenAether aether = new MavenAether();
        Settings settings = new Settings();

        Profile profile = new Profile();
        profile.setId("dev");
        Repository repository = new Repository();
        repository.setId(REPO_ID);
        repository.setUrl(REPO_URL);
        profile.addRepository(repository);
        settings.addProfile(profile);
        settings.addActiveProfile("dev");

        setSettings(aether, settings);

        DefaultRepositorySystemSession session = aether.newRepositorySystemSession();
        Object preemptive = session.getConfigProperties().get(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH);
        assertTrue(preemptive == null || Boolean.FALSE.equals(preemptive));
    }

    private void setSettings(MavenAether aether, Settings settings) throws Exception {
        Field settingsField = MavenAether.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(aether, settings);
    }
}
