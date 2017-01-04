# Aether Utils

Aether is a library written by the maven team to resolve dependencies. It allows dependency resolution
following the exact same rules as Maven in a standalone package. Unfortunately it's poorly documented
and difficult to wire up and use correctly in a standalone scenario. Aether Utils is a simple layer on
top of Aether it much easier to use:

    Aether aether = Aether.fromMavenSettings();    
    Artifact artifact = aether.resolveSingleArtifact("org.apache.commons:commons-lang3:RELEASE");

    List<Artifact> dependencies = aether.resolveDependencies(new DefaultArtifact("org.apache.commons:commons-lang3:3.3.2"));
