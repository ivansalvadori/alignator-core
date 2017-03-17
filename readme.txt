It is necessary to install external jars into local maven repository.
Required jars are available in libsToInstallMaven folder.
Execute the following commands: 
=================================================================+==
mvn install:install-file -Dfile=libsToInstallMaven/ontowrap.jar  -DgroupId=fr.inrialpes.exmo -DartifactId=ontowrap  -Dversion=2.5 -Dpackaging=jar
mvn install:install-file -Dfile=libsToInstallMaven/procalign.jar  -DgroupId=fr.inrialpes.exmo -DartifactId=procalign  -Dversion=2.5 -Dpackaging=jar
mvn install:install-file -Dfile=libsToInstallMaven/ontosim.jar  -DgroupId=fr.inrialpes.exmo -DartifactId=ontosim  -Dversion=2.5 -Dpackaging=jar
mvn install:install-file -Dfile=libsToInstallMaven/align.jar  -DgroupId=fr.inrialpes.exmo -DartifactId=align  -Dversion=2.5 -Dpackaging=jar
mvn install:install-file -Dfile=libsToInstallMaven/aroma.jar  -DgroupId=fr.inrialpes.exmo -DartifactId=aroma  -Dversion=1.2 -Dpackaging=jar
mvn install:install-file -Dfile=libsToInstallMaven/secondstring-20060615.jar  -DgroupId=com.wcohen.ss -DartifactId=secondstring  -Dversion=2.5 -Dpackaging=jar
