This plugin allows to deploy files specified in list file.

Ideal for deploy multiple artifacts in correct order.

I. Install plugin:

add to build.gradle
```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
        maven {
            url 'https://dl.bintray.com/nikit007/mvn-repo/'
        }
    }
    dependencies {
        classpath "com.github.nikit.cpp.helpers:db:1.0.6" // need for use helpers.MysqlHelper
        classpath "gradle.plugin.com.github.nikit.cpp:wildflyDeployPlugin:1.0.7"
    }
}
apply plugin: "com.github.nikit.cpp.wildfly.deploy"
```

II. Configure:

Firstly, create list file
```bash
mkdir scripts
vim scripts/deploy
```
add:
```
#Comments and

# empty lines are allowed
server/build/libs/server-0.1.jar
client/build/libs/client-0.1.war
ws/build/libs/webserwice-0.1.war
```
So, firstly will be deployed server-0.1.jar, further client-0.1.war, ...


Add to build.gradle:
```groovy
deployConfig {
    deployFile = "scripts/deploy" // list of files to deploy
    jbossHome = "/path/to/wildfly/home" // if null than will used system environment JBOSS_HOME or WILDFLY_HOME
    boxes = [
            'Local' : [
                    wildfly:new helpers.Server(),
                    beforeDeploy: {
                        String box, helpers.Server server ->
                        println "before hello box=${box}, server=${server}"
                        helpers.MysqlHelper.dropAndRestore(new helpers.Mysql(user:'root', pass:'root', dbName:'test', patches:['scripts/bootstrap.sql']))
                    },
                    afterUndeploy: { String box, helpers.Server server ->
                            println "after undeploy box=${box}, server=${server}"
                    }
            ],
            'LocalDomain' : [
                    wildfly:new helpers.Server(domain: true),
                    beforeDeploy: {
                        helpers.MysqlHelper.dropAndRestore(new helpers.Mysql(user: 'root', pass: 'root', dbName: 'test', patches: ['scripts/bootstrap.sql']))
                    }
            ],
            'MySuperWithoutDb': [
                    wildfly:new helpers.Server(username:'nikita', password:'qwerty', hostname:'192.168.1.200')
            ],
            'Dev': [
                    wildfly:new helpers.Server(username:'admin', password:'123', hostname:'192.168.1.10', domain: true,
                            domainServerGroups: ['main-server-group', 'other-server-group']),
                    before: {
                        println "runs always"
                        helpers.MysqlHelper.dropAndRestore(new helpers.Mysql(mysqlHost: '192.168.1.11', user: 'admin', pass: 'password', dbName: 'test',
                                patches: ['scripts/bootstrap.sql', 'scripts/dev.sql']))
                    }
            ]
    ]
}
```
As you see, you can add closures to box map with keys 'beforeDeploy', 'beforeUndeploy', 'afterDeploy', 'afterUndeploy'.

Closures with names 'before' and 'after' are executed always.

All all this closures are optional.

Also, all this closures can take parameters (String boxKey, helpers.Server server), (helpers.Server server), or none.

So, minimal configuration is
```groovy
deployConfig {
	deployFile = "scripts/deploy" // list of files to deploy
	boxes = [
		'Local' : [
			wildfly:new helpers.Server()
		]
	]
}
```
... for standalone local WildFly.


And, you can use artifactNamesClosure for automatic undeploy previous versions:
```groovy
String getArtifactName(helpers.Artifact a){
    int dash = helpers.StringUtils.rfind(a.displayName, '-' as char)
    if (dash == -1){
        return a.displayName
    }
    return a.displayName.substring(0, dash)
}

deployConfig {
    deployFile = "scripts/deploy" // list of files to deploy
    artifactNamesClosure = {
        helpers.JbossDeployer deployer,
        File file ->
            String fileName = file.name
            String newArtifactName = getArtifactName(new helpers.Artifact(displayName: file.name))
            Closure findPreviousVersionArtifactClosure = { helpers.Artifact a -> return getArtifactName(a) == newArtifactName }
            return [ runtimeName: fileName, displayName: fileName, undeployName: deployer.findOne(findPreviousVersionArtifactClosure)?.displayName]
    }
    boxes = [
		'Local' : [
			wildfly:new helpers.Server()
		]
	]
}
```

III. Usage

For previously defined boxes usage variants is:
```
gradle deployLocal
gradle undeployLocal
gradle redeployLocal
...
gradle deployMySuperWithoutDb
...
gradle undeployDev
...
```

redeployLocal is combination undeployLocal +  deployLocal

IV. Bugs/limitations
 * In current implementation of [JbossDeployer](https://github.com/nikit-cpp/helpers/blob/master/deployer/src/main/groovy/helpers/JbossDeployer.groovy) you cannot deploy file if file name is contains spaces, e. g. "/path/to/folder/my artifact.jar"
but "/path/to/my folder/artifact.jar" are deploys correctly.
