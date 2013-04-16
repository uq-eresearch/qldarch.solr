Legal Status and Licensing of Dependencies
==========================================

ALL third-party libraries, tools or other code MUST be listed here.

Raw original tarballs, zipfiles, or other downloads SHOULD be placed in dependencies/.

The tmp/ directory is in .gitignore, so makes a useful place to expand any archives to
allow extracting specific files into lib/, lib/build, or lib/test.

The four minimum fields required for any dependency are given in the template below:

Project:
URL:
License:
FILES:
JARS:

Project: JFlex
URL: http://jflex.de
License: GPLv2 or later
FILES: jflex-1.4.3.tar.gz
JARS: build/ant-contrib-1.0b3.jar

Project: Ant-Contrib
URL: http://ant-contrib.sourceforge.net/
License: Apache
FILES: ant-contrib-1.0b3-bin.zip 
JARS: build/ant-contrib-1.0b3.jar

Project: Apache Commons CLI
URL: http://commons.apache.org/proper/commons-cli/
License: Apache 2.0
FILES: commons-cli-1.2-bin.tar.gz
JARS: commons-cli-1.2.jar

Project: Apache Commons Codec
URL: http://commons.apache.org/proper/commons-codec/
License: Apache 2.0
FILES: commons-codec-1.4-bin.tar.gz
JARS: commons-codec-1.4.jar
NOTE: This project is very out of date, but a dependency for sesame.

Project: Apache Commons HttpClient
URL: http://hc.apache.org/httpclient-3.x/
License: Apache 2.0
FILES: commons-httpclient-3.1.tar.gz
JARS: commons-httpclient-3.1.jar
NOTE: This project is EOL but a dependency for sesame.

Project: Apache Commons IO
URL: http://commons.apache.org/proper/commons-io/
License: Apache 2.0
FILES: commons-io-2.4-bin.tar.gz
JARS: commons-io-2.4.jar

Project: Apache Commons Lang
URL: http://commons.apache.org/proper/commons-lang/
License: Apache 2.0
FILES: commons-lang3-3.1-bin.tar.gz
JARS: commons-lang3-3.1.jar

Project: Dom4J
URL: http://dom4j.sourceforge.net/
License: BSD 5-clause
FILES: commons-lang3-3.1-bin.tar.gz
JARS: commons-lang3-3.1.jar
NOTE: Trademark clause troublesome, but debian-legal appears to have accepted it.

Project: Junit
URL: http://junit.org/
License: CPL v1.0
FILES: junit-4.11.jar
JARS: test/junit-4.11.jar

Project: Logback
URL: http://logback.qos.ch/
License: Dual EPL v1.0/LGPL v2.1 or later
FILES: logback-1.0.11.tar.gz
JARS: logback-classic-1.0.11.jar logback-core-1.0.11.jar

Project: One-JAR
URL: http://one-jar.sourceforge.net/index.php?page=build-tools&file=ant
License: BSD
FILES: one-jar-ant-task-0.97.jar
JARS: build/one-jar-ant-task-0.97.jar

Project: Simple Logging Facade for Java
URL: http://www.slf4j.org/index.html
License: MIT
FILES: slf4j-1.7.5.tar.gz
JARS: jcl-over-slf4j-1.7.5.jar slf4j-api-1.7.5.jar

Project: Sesame
URL: http://www.openrdf.org/
License: BSD 3-clause
FILES: Compiled from source
JARS: openrdf-sesame-2.7-SNAPSHOT-onejar.jar

Project: Google Guava
URL: http://code.google.com/p/guava-libraries/
License: Apache 2.0
FILES: guava-14.0.1-javadoc.jar guava-14.0.1.jar
JARS: guava-14.0.1.jar

