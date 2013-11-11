# clj-projectr

A Clojure augmented reality simulation prototype. Currently both generating `src/clj_projectr/world.clj` (in the future
        simulating) the data and consuming it with libgdx `src/clj_projectr/core.clj`. Use `lein
run` to execute.

A motivational essay and presentation can be found `doc/`.

A clojure plugin for Eclipse is [Counterclockwise](https://code.google.com/p/counterclockwise/). Most other environments are supported as well.

## Usage

Java >= 6 and [Leiningen 2!](http://leiningen.org/) needs to be installed.

You need to fetch a recent version of libgdx from [nightlies](http://libgdx.badlogicgames.com/nightlies/).

Unpack into a subfolder and from there add it to your local maven
repository (Maven needs to be installed, tested on Ubuntu 13.04):

Adjust GDX_VERSION and run this snippet in bash in the subfolder:

    export GDX_VERSION="0.9.9-alpha20130513"
    for i in "gdx" "gdx-natives" "gdx-backend-lwjgl" "gdx-backend-lwjgl-natives"
    do
    mvn install:install-file -Durl=file:repo -DgroupId=com.badlogicgames -DartifactId=$i -Dversion=$GDX_VERSION -Dpackaging=jar -Dfile=$i.jar -e
    mvn deploy:deploy-file -Durl=file:repo -DgroupId=com.badlogicgames -DartifactId=$i -Dversion=$GDX_VERSION -Dpackaging=jar -Dfile=$i.jar -e
    done

Or this one in Powershell (thanks to Stephan!):

    $GDX_VERSION = "0.9.9-alpha20130513"
    $l = @("gdx", "gdx-natives", "gdx-backend-lwjgl", "gdx-backend-lwjgl-natives")
    $l | % {
	mvn install:install-file -Durl=file:repo -DgroupId="com.badlogicgames" -DartifactId="$_" -Dversion="$GDX_VERSION" -Dpackaging=jar -Dfile="$_.jar" -e ;
	mvn deploy:deploy-file -Durl=file:repo -DgroupId="com.badlogicgames" -DartifactId="$_" -Dversion="$GDX_VERSION" -Dpackaging=jar -Dfile="$_.jar" -e
    }


## License

Copyright © 2013 Christian Weilbach

Distributed under the Eclipse Public License, the same as Clojure.
