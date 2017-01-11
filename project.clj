(defproject projectr "0.1.0-SNAPSHOT"
  :description "Clojure simulation for ProjectR"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/core.incubator "0.1.4"]
                 [org.clojure/math.numeric-tower "0.0.4"]

                 [com.badlogicgames.gdx/gdx "1.9.3"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.9.3"]
                 [com.badlogicgames.gdx/gdx-box2d "1.9.3"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.9.3"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.9.3"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.9.3"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.9.3"
                  :classifier "natives-desktop"]

                 [ring "1.5.1"]
                 [ring-middleware-format "0.7.0"]
                 [compojure "1.5.2"]
                 [http-kit "2.2.0"]]
  :main projectr.core)
