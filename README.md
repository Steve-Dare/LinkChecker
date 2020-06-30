# LinkChecker

This utility checks markdown documentation for broken links.
It finds links by searching for matching brackets in markdown files in the provided path.

It checks:
1. External links resolve
2. Internal links do not end with a forward slash as that is not valid.
3. Internal links map to existing category/slug information in a markdown file in the provided path.

# Build
You can either build the repo as follows:
1. Clone this repo
2. `cd` into the cloned repo.
3. `mvn clean install`<br>
This builds a jar file in the target folder.

Or you can download the jar from [releases](https://github.com/Steve-Dare/LinkChecker/releases)

# Run
To run, enter the following command from command line:<br>
`java -jar brokenlinkfinder-1.0-SNAPSHOT.jar <docs-base-dir>`

e.g.<br>
`java -jar target/brokenlinkfinder-1.0-SNAPSHOT.jar $HOME/git/qp-docs/_10.0`

# Notes
1. It does not currently handle support, tutorial, api or connector docs as these are in a different path.
2. It ignores any links containing images (.png, .svg, .pdf).

These could be fixed in future enhancements.
