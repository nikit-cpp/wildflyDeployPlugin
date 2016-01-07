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
        classpath "com.github.nikit.cpp.helpers:db:1.0" // need for use helpers.MysqlHelper
        classpath "gradle.plugin.com.github.nikit.cpp:wildflyDeployPlugin:1.0.3"
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
server/build/libs/server.jar
client/build/libs/client.war
ws/build/libs/webserwice.war
```
So, firstly will be deployed server.jar, further client.war, ...


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
                    },
                    afterRedeploy: { println "closure parameters are optional" }
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
As you see, you can add closures to box map with keys 'beforeDeploy', 'beforeRedeploy', 'beforeUndeploy',  'afterDeploy', 'afterRedeploy', 'afterUndeploy'.

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

III. Usage

For previously defined boxes usage variants is:
```groovy
gradle deployLocal
gradle undeployLocal
gradle redeployLocal
...
gradle deployMySuperWithoutDb
...
gradle undeployDev
...
```
IV. Bugs/limitations
 * In current implementation of [JbossDeployer](https://github.com/nikit-cpp/helpers/blob/master/deployer/src/main/groovy/helpers/JbossDeployer.groovy) you cannot deploy file if path is contains spaces, e. g. "/path/to/my folder/artifact.jar"
