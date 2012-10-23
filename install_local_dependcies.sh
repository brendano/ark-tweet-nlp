#!/bin/bash
mvn install:install-file  -Dfile=lib/jargs.jar -DgroupId=edu.cmu.cs.jargs -DartifactId=jargs -Dversion=2010-08-30 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file  -Dfile=lib/posBerkeley.jar -DgroupId=edu.cmu.cs -DartifactId=edu.berkeley.nlp.posBerkeley -Dversion=custom -Dpackaging=jar -DgeneratePom=true
mvn install:install-file  -Dfile=lib/stanford-postagger-2010-05-26.jar -DgroupId=edu.cmu.cs -DartifactId=edu.stanford.nlp.postagger -Dversion=2010-05-26 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file  -Dfile=lib/trove-3.0.0a5.jar -DgroupId=edu.cmu.cs -DartifactId=gnu.trove -Dversion=3.0.0a5 -Dpackaging=jar -DgeneratePom=true
