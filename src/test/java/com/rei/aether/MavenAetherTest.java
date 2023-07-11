package com.rei.aether;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class MavenAetherTest {
    @Test
    public void canResolveSingleDependency() {
        Aether aether = Aether.fromMavenSettings();
        aether.getConfiguredRepositories().forEach(System.out::println);
        Artifact artifact = aether.resolveSingleArtifact("org.junit.jupiter:junit-jupiter-api:RELEASE");
        assertNotNull(artifact);
    }

    @Test
    public void canResolveDependencies() {
        Aether aether = Aether.fromMavenSettings();
        List<Artifact> dependencies = aether.resolveDependencies(new DefaultArtifact("org.junit.jupiter:junit-jupiter-api:5.9.3"));
        assertFalse(dependencies.isEmpty());
        dependencies.forEach(d -> {
            System.out.println(d);
            assertNotNull(d.getFile());
        });
    }
}
