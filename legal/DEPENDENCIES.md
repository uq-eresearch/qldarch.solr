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

Project: Junit
URL: http://junit.org/
License: CPL v1.0
FILES: junit-4.11.jar, junit-4.11-javadoc.jar

Project: Ant-Contrib
URL: http://ant-contrib.sourceforge.net/
License: Apache
FILES: ant-contrib-1.0b3-bin.zip, 

Project: One-JAR
URL: http://one-jar.sourceforge.net/index.php?page=build-tools&file=ant
License: BSD
FILES: one-jar-ant-task-0.97.jar

Project: Logback
URL: http://logback.qos.ch/
License: Dual EPL v1.0/LGPL v2.1
FILES: logback-1.0.9.tar.gz

