$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
java -classpath "$PSScriptRoot\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain $args
