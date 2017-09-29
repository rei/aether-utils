package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.slf4j.LoggerFactory;

public abstract class Aether {
    private RepositorySystem repositorySystem;
    
    public Artifact resolveSingleArtifact(String gavSpec) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(gavSpec));
        request.setRepositories(getConfiguredRepositories());

        try {
            return getRepositorySystem().resolveArtifact(getRespositorySystemSession(), request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new AetherException(e);
        }
    }
    
    public List<Artifact> resolveDependencies(String gavSpec) {
        return resolveDependencies(new DefaultArtifact(gavSpec), JavaScopes.RUNTIME);
    }
    
    public List<Artifact> resolveDependencies(String gavSpec, String scope) {
        return resolveDependencies(new DefaultArtifact(gavSpec), scope);
    }
    
    public List<Artifact> resolveDependencies(Artifact artifact) {
        return resolveDependencies(artifact, JavaScopes.RUNTIME);
    }
    
    public List<Artifact> resolveDependencies(Artifact artifact, String scope) {
        try {
            RepositorySystemSession session = getRespositorySystemSession();
            
            ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest()
                    .setArtifact(artifact)
                    .setRepositories(getConfiguredRepositories());
            
            ArtifactDescriptorResult descriptorResult = getRepositorySystem().readArtifactDescriptor(session, descriptorRequest);
            
            CollectRequest request = new CollectRequest()
                    .setRoot(new Dependency(artifact, scope))
                    .setDependencies(descriptorResult.getDependencies())
                    .setManagedDependencies(descriptorResult.getManagedDependencies())
                    .setRepositories(getConfiguredRepositories());
    
            DependencyRequest dependencyRequest = new DependencyRequest(request, DependencyFilterUtils.classpathFilter(scope));
    
            return getRepositorySystem().resolveDependencies(session, dependencyRequest).getArtifactResults().stream()
                                        .map(r -> r.getArtifact())
                                        .collect(toList());
            
        } catch (ArtifactDescriptorException | org.eclipse.aether.resolution.DependencyResolutionException e) {
            throw new AetherException(e);
        }
    }
    
    protected RepositorySystem getRepositorySystem() {
        if (repositorySystem == null) {
            repositorySystem = newRepositorySystem();
        }
        return repositorySystem;
    }
    
    private RepositorySystemSession getRespositorySystemSession() {
        DefaultRepositorySystemSession session = newRepositorySystemSession();
        session.setRepositoryListener(new LoggingRepositoryListener(LoggerFactory.getLogger(getClass())));
        session.setTransferListener(new LoggingTransferListener(LoggerFactory.getLogger(getClass())));
        return session;
    }
    
    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable e) {
                throw new AetherException(e);
            }
        });

        return locator.getService(RepositorySystem.class);    
    }

    public abstract List<RemoteRepository> getConfiguredRepositories();
    protected abstract DefaultRepositorySystemSession newRepositorySystemSession();

    public static Aether fromMavenSettings() {
        return new MavenAether();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Map<String, String> remoteRepos = new HashMap<>();
        private Path localRepo;
        
        public Builder setDefaultRemoteRepo(String url) {
            return addRemoteRepo("default", url);
        }
        
        public Builder addRemoteRepo(String id, String url) {
            remoteRepos.put(id, url);
            return this;
        }
        
        public Builder setLocalRepo(String path) {
            return setLocalRepo(Paths.get(path));
        }
        
        public Builder setTempLocalRepo() {
            try {
                return setLocalRepo(Files.createTempDirectory("aether-repo"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        public Builder setLocalRepo(Path path) {
            localRepo = path;
            return this;
        }
        
        public Aether build() {
            return new ConfiguredAether(remoteRepos, localRepo);
        }
    }
}
