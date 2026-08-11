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
    private final boolean preemptiveAuth;

    ConfiguredAether(Map<String, String> remoteRepos, Path localRepo, String username, String password,
            boolean preemptiveAuth) {
        this.repos = remoteRepos.entrySet().stream()
                .map(e -> buildRepository(e.getKey(), e.getValue(), username, password))
                .collect(toList());
        this.localRepo = new LocalRepository(localRepo.toFile());
        this.preemptiveAuth = preemptiveAuth;
    }

    private static RemoteRepository buildRepository(String id, String url, String username, String password) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, "default", url);
        if (hasCredentials(username, password)) {
            Authentication auth = new AuthenticationBuilder()
                    .addUsername(username)
                    .addPassword(password)
                    .build();
            builder.setAuthentication(auth);
        }
        return builder.build();
    }

    private static boolean hasCredentials(String username, String password) {
        return username != null && !username.isEmpty() && password != null;
    }

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, localRepo));
        if (preemptiveAuth) {
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
