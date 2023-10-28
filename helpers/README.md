# Helper Jars

Files to be packed into helper jars that are injected into the classpath during compilation.

## [checker](./checker/)

Contains an [annotation service file](./checker/META-INF/services/javax.annotation.processing.Processor) that lists all enabled checker classes. Included when the `lint` profile is used.

See the full checker list at https://checkerframework.org/manual.