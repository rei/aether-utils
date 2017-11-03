package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

class ConfiguredAether extends Aether {

    private List<RemoteRepository> repos;
    private LocalRepository localRepo;

    ConfiguredAether(Map<String, String> remoteRepos, Path localRepo) {
        this.repos = remoteRepos.entrySet().stream()
                .map(e -> new RemoteRepository.Builder(e.getKey(), "default", e.getValue()).build())
                .collect(toList());
        this.localRepo = new LocalRepository(localRepo.toFile());
    }

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        
        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, localRepo));
        
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
