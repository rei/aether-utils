package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

class MavenAether extends Aether {
    private static final Logger logger = LoggerFactory.getLogger(MavenAether.class);

    public static final String userHome = System.getProperty("user.home");
    public static final File userMavenConfigurationHome = new File(userHome, ".m2");
    public static final String envM2Home = System.getenv("M2_HOME");
    public static final File DEFAULT_USER_SETTINGS_FILE = new File(userMavenConfigurationHome, "settings.xml");
    public static final File DEFAULT_USER_LOCAL_REPOS = new File(userMavenConfigurationHome, "repository");

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File(
            System.getProperty("maven.home", envM2Home != null ? envM2Home : ""), "conf/settings.xml");

    private Settings settings;
    private List<RemoteRepository> configuredRepositories;

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, getLocalRepository()));
        if (hasAuthentication()) {
            session.setConfigProperty(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH, true);
        }
        return session;
    }

    @Override
    public LocalRepository getLocalRepository() {
        return Optional.ofNullable(getSettings().getLocalRepository()).map(LocalRepository::new)
                       .orElse(new LocalRepository(DEFAULT_USER_LOCAL_REPOS));
    }

    @Override
    public List<RemoteRepository> getConfiguredRepositories() {
        if (configuredRepositories == null) {
            configuredRepositories = buildConfiguredRepositories();
        }
        return configuredRepositories;
    }

    private List<RemoteRepository> buildConfiguredRepositories() {
        Map<String, Profile> profilesMap = getSettings().getProfilesAsMap();
        List<RemoteRepository> remotes = new ArrayList<>();

        for (String profileName : getSettings().getActiveProfiles()) {
            Profile profile = profilesMap.get(profileName);
            if (profile == null) {
                continue;
            }
            List<org.apache.maven.settings.Repository> repositories = profile.getRepositories();
            if (repositories == null) {
                continue;
            }
            for (org.apache.maven.settings.Repository repo : repositories) {
                remotes.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
            }
        }

        final DefaultMirrorSelector selector = new DefaultMirrorSelector();

        Optional.ofNullable(settings.getMirrors()).ifPresent(mirrors ->
            mirrors.forEach(m ->
                selector.add(m.getId(), m.getUrl(), m.getLayout(), false, false, m.getMirrorOf(), m.getMirrorOfLayouts())));

        return remotes.stream()
                      .map(r -> Optional.ofNullable(selector.getMirror(r)).orElse(r))
                      .map(this::withAuthentication)
                      .collect(toList());
    }

    private RemoteRepository withAuthentication(RemoteRepository repo) {
        Authentication auth = getAuthentication(repo.getId());
        if (auth == null) {
            return repo;
        }
        return new RemoteRepository.Builder(repo).setAuthentication(auth).build();
    }

    private Authentication getAuthentication(String serverId) {
        Server server = getSettings().getServer(serverId);
        if (server == null) {
            return null;
        }

        String username = server.getUsername();
        String password = server.getPassword();
        if (username == null || username.isEmpty() || password == null) {
            return null;
        }

        String decryptedPassword = decryptPassword(password);
        return new AuthenticationBuilder()
                .addUsername(username)
                .addPassword(decryptedPassword)
                .build();
    }

    private boolean hasAuthentication() {
        return getConfiguredRepositories().stream().anyMatch(r -> r.getAuthentication() != null);
    }

    private String decryptPassword(String password) {
        if (password == null) {
            return null;
        }
        try {
            PlexusCipher cipher = new DefaultPlexusCipher();
            DefaultSecDispatcher secDispatcher = new DefaultSecDispatcher(cipher);
            secDispatcher.setConfigurationFile(new File(userMavenConfigurationHome, "settings-security.xml").getPath());
            DefaultSettingsDecrypter decrypter = new DefaultSettingsDecrypter(secDispatcher);
            Server server = new Server();
            server.setPassword(password);
            SettingsDecryptionResult result = decrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
            Server decryptedServer = result.getServer();
            if (decryptedServer != null && decryptedServer.getPassword() != null) {
                return decryptedServer.getPassword();
            }
        } catch (Exception e) {
            logger.warn("Unable to decrypt Maven settings password, using value as-is", e);
        }
        return password;
    }

    private Settings getSettings() {
        if (settings == null) {
            try {
                SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest()
                        .setSystemProperties(System.getProperties()).setUserSettingsFile(DEFAULT_USER_SETTINGS_FILE)
                        .setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);

                settings = new DefaultSettingsBuilderFactory().newInstance().build(settingsBuildingRequest)
                        .getEffectiveSettings();
            } catch (SettingsBuildingException e) {
                throw new IllegalStateException(e);
            }
        }
        return settings;
    }
}
