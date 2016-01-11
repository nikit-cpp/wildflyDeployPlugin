package com.github.nikit.cpp.wildfly

import helpers.JbossDeployer
import helpers.Server
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

import static org.testng.Assert.assertTrue

/**
 * Created by nik on 10.01.16.
 */
class WildflyDeployPluginTest {
    @Test
    public void greeterPluginAddsGreetingTaskToProject() {

        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'com.github.nikit.cpp.wildfly.deploy'

        project.deployConfig {
            deployFile = "src/test/resources/deploy.list" // list of files to deploy
            boxes = [
                    'Local' : [
                            wildfly:new helpers.Server()
                    ]
            ]
        }

        Assert.assertNotNull(project.tasks.findByName('deployLocal'))

        project.tasks["deployLocal"].execute()
    }
}

class MockDeployer extends JbossDeployer{

    MockDeployer(Server server, String jbossHome, Closure createArtifactNamesClosure, boolean force) {
        super(server, jbossHome, createArtifactNamesClosure, force)
    }
}
