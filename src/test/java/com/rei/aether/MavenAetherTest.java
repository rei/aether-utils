package com.rei.aether;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

public class MavenAetherTest {
    @Test
    public void canResolveSingleDependency() {
        Aether aether = Aether.fromMavenSettings();
        aether.getConfiguredRepositories().forEach(System.out::println);
        Artifact artifact = aether.resolveSingleArtifact("com.rei.devops:bigip-client:1.13");
        assertNotNull(artifact);
    }
    
    @Test
    public void canResolveDependencies() {
        Aether aether = Aether.fromMavenSettings();
        List<Artifact> dependencies = aether.resolveDependencies(new DefaultArtifact("com.rei.devops:bigip-client:1.13"));
        assertFalse(dependencies.isEmpty());
        dependencies.forEach(d -> {
            System.out.println(d);
            assertNotNull(d.getFile());
        });
    }
}
