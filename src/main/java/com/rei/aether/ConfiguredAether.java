package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

class ConfiguredAether extends Aether {

    private final List<RemoteRepository> repos;
    private final LocalRepository localRepo;
    private final boolean authConfigured;

    ConfiguredAether(Map<String, RepoConfig> remoteRepos, Path localRepo) {
        this.repos = remoteRepos.entrySet().stream()
                .map(e -> buildRepository(e.getKey(), e.getValue()))
                .collect(toList());
        this.localRepo = new LocalRepository(localRepo.toFile());
        this.authConfigured = remoteRepos.values().stream().anyMatch(RepoConfig::hasCredentials);
    }

    private static RemoteRepository buildRepository(String id, RepoConfig config) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, "default", config.url);
        if (config.hasCredentials()) {
            Authentication auth = new AuthenticationBuilder()
                    .addUsername(config.username)
                    .addPassword(config.password)
                    .build();
            builder.setAuthentication(auth);
        }
        return builder.build();
    }

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, localRepo));

        if (authConfigured) {
            session.setConfigProperty(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH, true);
        }

        return session;
    }

    @Override
    public LocalRepository getLocalRepository() {
        return localRepo;
    }

    @Override
    public List<RemoteRepository> getConfiguredRepositories() {
        return repos;
    }

}
