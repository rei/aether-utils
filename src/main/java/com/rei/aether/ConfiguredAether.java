package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

public class ConfiguredAether extends Aether {

    private List<RemoteRepository> repos;
    private Path localRepo;

    public ConfiguredAether(Map<String, String> remoteRepos, Path localRepo) {
        this.repos = remoteRepos.entrySet().stream()
                .map(e -> new RemoteRepository.Builder(e.getKey(), "default", e.getValue()).build())
                .collect(toList());
        this.localRepo = localRepo;
    }

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        
        session.setLocalRepositoryManager(getRepositorySystem()
                    .newLocalRepositoryManager(session, new LocalRepository(localRepo.toFile())));
        
        return session;
    }

    @Override
    protected List<RemoteRepository> getConfiguredRepositories() {
        return repos;
    }

}
