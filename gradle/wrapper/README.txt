gradle-wrapper.jar is a binary file and is generated automatically.
Android Studio recreates it on the first Gradle sync. If building from the
command line without Android Studio, run once:  gradle wrapper --gradle-version 8.9
(using a locally installed Gradle), which produces gradle/wrapper/gradle-wrapper.jar.
