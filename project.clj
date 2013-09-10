(defproject projectr "0.1.0-SNAPSHOT"
  :description "Clojure simulation for ProjectR"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/core.incubator "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [org.clojure/tools.trace "0.7.5"]

                 [com.badlogicgames/gdx "0.9.9-alpha20130513"]
                 [com.badlogicgames/gdx-natives "0.9.9-alpha20130513"]
                 [com.badlogicgames/gdx-backend-lwjgl "0.9.9-alpha20130513"]
                 [com.badlogicgames/gdx-backend-lwjgl-natives "0.9.9-alpha20130513"]


                 [ring "1.2.0"]
                 [ring-middleware-format "0.3.0"]
                 [compojure "1.1.5"]
                 [http-kit "2.1.8"]]
  :repositories {"project" "file:repo"}
  :main projectr.core)
