#Info
This plugin adds `providedCompile` configuration if not exist,
prepares WildFly deploymets recurcively walks on dependency
tree - copy dependency jar to `build\dependency-workspace` and
adds Dependencies string to theirs maifests.
Also plugin can copy dependency jar to WildFly deploymets directory.

#TODO
* in task deployDeployments make deploys via jboss-cli

#Usage
1. Install plugin:  
```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.github.nikit.cpp:wildflyPlugin:0.9"
  }
}
apply plugin: "com.github.nikit.cpp.wildfly"
```
2. Add WildFly extension:  
```
wildfly {
	deploymentDestination = 'C:\\path\\to\\wildfly\\standalone\\deployments' // or '${WILDFLY_HOME}\\standalone\\deployments'
	addFirstLevelDependenciesToManifest = true // generate manifest and add firstLevel dependencies
	printTree = false // print dependency tree for debug purposes
}
```
3. If nesserary change `compile` configuration to `providedCompile` for those dependencies that are prolided by WildFly,
for example:  
```
dependencies {
	compile 'org.apache.qpid:qpid-client:0.32'
	providedCompile 'org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec:1.0.0.Final'
	providedCompile 'org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec:1.0.0.Final'
}
```

#Gradle tasks
* `gradle modules` copy all dependent jars into `C:\Path\To\Project\build\dependency-workspace`
and makes modules template from it (you have to add common Java EE dependencies, e. g. javax.api ...)