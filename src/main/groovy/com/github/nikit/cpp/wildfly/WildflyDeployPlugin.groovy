package com.github.nikit.cpp.wildfly

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

/**
 * Created by nik on 12.05.15.
 */
class WildflyDeployPlugin implements Plugin<Project> {
    String extensionName = 'deployConfig'

    Map boxes
    String deployFile
    String jbossHome
    Closure createArtifactNamesClosure

    void apply(Project project) {
        ExtensionContainer ec = project.extensions
        ec.create(extensionName, WildflyPluginExtension)

        project.tasks.addRule("Pattern: [re|un]deploy<ID>") { String taskName ->
            boxes = project[extensionName].boxes
            deployFile = project[extensionName].deployFile
            jbossHome = project[extensionName].jbossHome
            createArtifactNamesClosure = project[extensionName].artifactNamesClosure

            if (taskName.startsWith("redeploy")) {
                def envMatcher = (taskName =~ /redeploy(.*)/)
                envMatcher.find()

                project.tasks.create(taskName) << {
                    def env_name = envMatcher.group(1)
                    println "Selected box : ${env_name}"
                    helpers.Server server = boxes[env_name].wildfly

                    Closure beforeClosure = boxes[env_name].before
                    Closure afterClosure = boxes[env_name].after

                    Closure beforeRedeployClosure = boxes[env_name].beforeRedeploy
                    Closure afterRedeployClosure = boxes[env_name].afterRedeploy

                    callClosureWithMultipleParameters(beforeClosure, server, env_name)
                    callClosureWithMultipleParameters(beforeRedeployClosure, server, env_name)
                    redeploy(server)
                    callClosureWithMultipleParameters(afterRedeployClosure, server, env_name)
                    callClosureWithMultipleParameters(afterClosure, server, env_name)
                }
            }

            if (taskName.startsWith("deploy")) {
                def envMatcher = (taskName =~ /deploy(.*)/)
                envMatcher.find()

                project.tasks.create(taskName) << {
                    def env_name = envMatcher.group(1)
                    println "Selected box : ${env_name}"
                    helpers.Server server = boxes[env_name].wildfly

                    Closure beforeClosure = boxes[env_name].before
                    Closure afterClosure = boxes[env_name].after

                    Closure beforeDeployClosure = boxes[env_name].beforeDeploy
                    Closure afterDeployClosure = boxes[env_name].afterDeploy

                    callClosureWithMultipleParameters(beforeClosure, server, env_name)
                    callClosureWithMultipleParameters(beforeDeployClosure, server, env_name)
                    deploy(server)
                    callClosureWithMultipleParameters(afterDeployClosure, server, env_name)
                    callClosureWithMultipleParameters(afterClosure, server, env_name)
                }
            }

            if (taskName.startsWith("undeploy")) {
                def envMatcher = (taskName =~ /undeploy(.*)/)
                envMatcher.find()

                project.tasks.create(taskName) << {
                    def env_name = envMatcher.group(1)
                    println "Selected box : ${env_name}"
                    helpers.Server server = boxes[env_name].wildfly

                    Closure beforeClosure = boxes[env_name].before
                    Closure afterClosure = boxes[env_name].after

                    Closure beforeUndeployClosure = boxes[env_name].beforeUndeploy
                    Closure afterUndeployClosure = boxes[env_name].afterUndeploy

                    callClosureWithMultipleParameters(beforeClosure, server, env_name)
                    callClosureWithMultipleParameters(beforeUndeployClosure, server, env_name)
                    undeploy(server)
                    callClosureWithMultipleParameters(afterUndeployClosure, server, env_name)
                    callClosureWithMultipleParameters(afterClosure, server, env_name)
                }
            }
        }

    }
    
    void callClosureWithMultipleParameters(Closure closure, helpers.Server server, String envName){
        if(null==closure){
            return
        }
        switch (closure.maximumNumberOfParameters){
            case 0:
                closure.call()
                break;
            case 1:
                closure.call(server)
                break;
            case 2:
                closure.call(envName, server)
                break;
            default:
                throw new RuntimeException('Alowed: "String boxKey, helpers.Server server", "helpers.Server server", or none')
        }

    }

    void redeploy ( helpers.Server server ) {
        helpers.JbossDeployer deployer = new helpers.JbossDeployer(server, jbossHome, createArtifactNamesClosure)
        deployer.readFile(deployFile)
        deployer.undeployList()
        deployer.deployList()
    }

    void deploy ( helpers.Server server ) {
        helpers.JbossDeployer deployer = new helpers.JbossDeployer(server, jbossHome, createArtifactNamesClosure)
        deployer.readFile(deployFile)
        deployer.deployList()
    }

    void undeploy ( helpers.Server server ) {
        helpers.JbossDeployer deployer = new helpers.JbossDeployer(server, jbossHome, createArtifactNamesClosure)
        deployer.readFile(deployFile)
        deployer.undeployList()
    }

}

class WildflyPluginExtension {
    Map boxes
    String deployFile
    String jbossHome
    Closure artifactNamesClosure
}
