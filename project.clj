(defproject everything-is-diamond "1.0.0"
  :description "DUANG!"
  :url "https://github.com/iovxw/everything-is-diamond"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :repositories [["spigot-repo" "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"]]
  :profiles {:provided
             {:dependencies
              [[org.bukkit/bukkit "1.9-R0.1-SNAPSHOT"]]}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]]
  :aot :all)
