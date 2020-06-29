# LinkChecker

This utility checks documentation for broken links.

It checks:
1. External links resolve
2. Internal links do not end with a forward slash as that is not valid.
3. Internal links resolve

To run:
1. Clone this repo
2. Change path as required
3. `mvn clean`
4. `mvn install`
5. `java -jar target/brokenlinkfinder-1.0-SNAPSHOT.jar`

