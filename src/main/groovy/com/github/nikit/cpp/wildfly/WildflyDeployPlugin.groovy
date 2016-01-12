package com.github.nikit.cpp.wildfly

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

/**
 * Created by nik on 12.05.15.
 */
class WildflyDeployPlugin implements Plugin<Project> {

    public static final String REDEPLOY = "redeploy"
    public static final String DEPLOY = "deploy"
    public static final String UNDEPLOY = "undeploy"


    String extensionName = 'deployConfig'

    Map boxes
    String deployFile
    String jbossHome
    Closure createArtifactNamesClosure
    boolean force

    void apply(Project project) {
        ExtensionContainer ec = project.extensions
        ec.create(extensionName, WildflyPluginExtension)

        project.tasks.addRule("Pattern: [re|un]deploy<ID>") { String taskName ->
            boxes = project[extensionName].boxes
            deployFile = project[extensionName].deployFile
            jbossHome = project[extensionName].jbossHome
            createArtifactNamesClosure = project[extensionName].artifactNamesClosure
            force = (project[extensionName].force==null) ? false : project[extensionName].force

            if (taskName.startsWith(REDEPLOY)) {
                project.tasks.create(taskName) << {
                    def env_name = getEnvName(taskName, REDEPLOY)

                    println "Selected box : ${env_name}"
                    helpers.Server server = boxes[env_name].wildfly

                    Closure beforeClosure = boxes[env_name].before
                    Closure afterClosure = boxes[env_name].after

                    Closure beforeDeployClosure = boxes[env_name].beforeDeploy
                    Closure afterDeployClosure = boxes[env_name].afterDeploy
                    Closure beforeUndeployClosure = boxes[env_name].beforeUndeploy
                    Closure afterUndeployClosure = boxes[env_name].afterUndeploy

                    callClosureWithMultipleParameters(beforeClosure, server, env_name)

                    callClosureWithMultipleParameters(beforeUndeployClosure, server, env_name)
                    undeploy(server)
                    callClosureWithMultipleParameters(afterUndeployClosure, server, env_name)

                    callClosureWithMultipleParameters(beforeDeployClosure, server, env_name)
                    deploy(server)
                    callClosureWithMultipleParameters(afterDeployClosure, server, env_name)

                    callClosureWithMultipleParameters(afterClosure, server, env_name)
                }
            }

            if (taskName.startsWith(DEPLOY)) {
                project.tasks.create(taskName) << {
                    def env_name = getEnvName(taskName, DEPLOY)

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

            if (taskName.startsWith(UNDEPLOY)) {
                project.tasks.create(taskName) << {
                    def env_name = getEnvName(taskName, UNDEPLOY)

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

    String getEnvName (String taskName, String pattern){
        def envMatcher = (taskName =~ /${pattern}(.*)/)
        envMatcher.find()
        def env_name = envMatcher.group(1)
        return env_name
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

    void deploy ( helpers.Server server ) {
        helpers.JbossDeployer deployer = new helpers.JbossDeployer(server, jbossHome, createArtifactNamesClosure, force)
        deployer.readFile(deployFile)
        deployer.deployList()
    }

    void undeploy ( helpers.Server server ) {
        helpers.JbossDeployer deployer = new helpers.JbossDeployer(server, jbossHome, createArtifactNamesClosure, force)
        deployer.readFile(deployFile)
        deployer.undeployList()
    }

}

class WildflyPluginExtension {
    boolean force
    Map boxes
    String deployFile
    String jbossHome
    Closure artifactNamesClosure
}
