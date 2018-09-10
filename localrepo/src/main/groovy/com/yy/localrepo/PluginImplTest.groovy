package com.yy.localrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator
import org.gradle.api.internal.artifacts.mvnsettings.MavenSettingsProvider
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.MavenPlugin
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.Factory
import org.gradle.internal.logging.LoggingManagerInternal

public class PluginImplTest extends MavenPlugin{

    PluginImplTest(Factory<LoggingManagerInternal> loggingManagerFactory, FileResolver fileResolver, ProjectPublicationRegistry publicationRegistry, ProjectConfigurationActionContainer configurationActionContainer, MavenSettingsProvider mavenSettingsProvider, LocalMavenRepositoryLocator mavenRepositoryLocator, ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
        super(loggingManagerFactory, fileResolver, publicationRegistry, configurationActionContainer, mavenSettingsProvider, mavenRepositoryLocator, moduleIdentifierFactory)
    }

    @Override
    public void apply(final ProjectInternal project) {

    }
}