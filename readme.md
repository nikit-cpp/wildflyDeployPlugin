This plugin allows to deploy files specified in list file

I. Install plugin:

add to build.gradle
```
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
        classpath "gradle.plugin.com.github.nikit.cpp:wildflyDeployPlugin:1.0.1"
    }
}
apply plugin: "com.github.nikit.cpp.wildfly.deploy"
```

II. Configure:

Firstly, create list file
```
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

to build.gradle:
```
deployConfig {
    deployFile = "scripts/deploy", // list of files to deploy
    jbossHome = "/path/to/wildfly/home", // if null than will used system environment JBOSS_HOME or WILDFLY_HOME
    boxes = [
            'Local' : [
                    wildfly:new helpers.Server(),
                    before: {
                        String box, helpers.Server server ->
                        println "before hello box=${box}, server=${server}"
                        helpers.MysqlHelper.dropAndRestore(new helpers.Mysql(user:'root', pass:'root', dbName:'test', patches:['scripts/bootstrap.sql']))
                    },
                    after: { String box, helpers.Server server ->
                            println "after goodbye box=${box}, server=${server}"
                    }
            ],
            'LocalDomain' : [
                    wildfly:new helpers.Server(domain: true),
                    before: {
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
                        helpers.MysqlHelper.dropAndRestore(new helpers.Mysql(mysqlHost: '192.168.1.11', user: 'admin', pass: 'password', dbName: 'test',
                                patches: ['scripts/bootstrap.sql', 'scripts/dev.sql']))
                    }
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