package com.rei.aether;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

class MavenAether extends Aether {
    public static final String userHome = System.getProperty("user.home");
    public static final File userMavenConfigurationHome = new File(userHome, ".m2");
    public static final String envM2Home = System.getenv("M2_HOME");
    public static final File DEFAULT_USER_SETTINGS_FILE = new File(userMavenConfigurationHome, "settings.xml");
    public static final File DEFAULT_USER_LOCAL_REPOS = new File(userMavenConfigurationHome, "repository");

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File(
            System.getProperty("maven.home", envM2Home != null ? envM2Home : ""), "conf/settings.xml");

    private Settings settings;

    @Override
    protected DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = Optional.ofNullable(getSettings().getLocalRepository()).map(LocalRepository::new)
                                            .orElse(new LocalRepository(DEFAULT_USER_LOCAL_REPOS));
        
        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, localRepo));

        return session;
    }

    @Override
    protected List<RemoteRepository> getConfiguredRepositories() {
        Map<String, Profile> profilesMap = getSettings().getProfilesAsMap();
        List<RemoteRepository> remotes = new ArrayList<>();

        for (String profileName : getSettings().getActiveProfiles()) {
            Profile profile = profilesMap.get(profileName);
            List<org.apache.maven.settings.Repository> repositories = profile.getRepositories();
            for (org.apache.maven.settings.Repository repo : repositories) {
                remotes.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
            }
        }

        final DefaultMirrorSelector selector = new DefaultMirrorSelector();
        
        Optional.ofNullable(settings.getMirrors()).ifPresent(mirrors -> 
            mirrors.forEach(m -> 
                selector.add(m.getId(), m.getUrl(), m.getLayout(), false, m.getMirrorOf(), m.getMirrorOfLayouts())));

        return remotes.stream().map(r -> Optional.ofNullable(selector.getMirror(r)).orElse(r)).collect(toList());
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
