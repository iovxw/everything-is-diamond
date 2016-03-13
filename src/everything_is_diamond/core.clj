(ns everything-is-diamond.core
  (:require [clojure.data.json :as json])
  (:import [org.bukkit.block BlockFace Hopper Sign]
           [org.bukkit Bukkit Location Material ChatColor]
           [org.bukkit.inventory ItemStack]
           [java.io File])
  (:gen-class :name everything_is_diamond.core
              :extends org.bukkit.plugin.java.JavaPlugin
              :implements [org.bukkit.event.Listener]
              :methods [[^{org.bukkit.event.EventHandler true}
                         onSignChange [org.bukkit.event.block.SignChangeEvent] void]]))

(def logger (atom nil))
(def data-file (atom ""))
(def machine-list (atom #{}))

(defn log-info [msg]
  (.info @logger msg))
(defn log-config [msg]
  (.config @logger msg))
(defn log-waring [msg]
  (.waring @logger msg))
(defn log-fine [msg]
  (.fine @logger msg))
(defn log-finer [msg]
  (.finer @logger msg))
(defn log-finest [msg]
  (.finest @logger msg))
(defn log-severe [msg]
  (.severe @logger msg))
(defn log-throwing [source-class source-method thrown]
  (.throwing @logger source-class source-method thrown))

(def face [BlockFace/EAST BlockFace/NORTH BlockFace/SOUTH BlockFace/WEST])

(defn is-machine [sign-block]
  (when (= (.getType sign-block) Material/WALL_SIGN)
    (some #(let [c-block (.getRelative sign-block %)]
             (when (= (.getType c-block) Material/EMERALD_BLOCK)
               (let [t-hopper (.getRelative c-block BlockFace/UP)
                     b-hopper (.getRelative c-block BlockFace/DOWN)]
                 (when (and (= (.getType t-hopper) Material/HOPPER)
                            (= (.getType b-hopper) Material/HOPPER)
                            (= (.getData t-hopper) 0))
                   ; .getData: 0=Facing down, 1=unattached to any container
                   ; 2=Facing North, 3=Facing South, 4=Facing West, 5=Facing East
                   {:sign (cast Sign (.getState sign-block)) :core c-block
                    :t-hopper (cast Hopper (.getState t-hopper))
                    :b-hopper (cast Hopper (.getState b-hopper))}))))
          face)))

(defn add-machine [world x y z]
  (swap! machine-list conj [world x y z])
  (spit @data-file (json/write-str @machine-list)))
(defn del-machine [world x y z]
  (swap! machine-list disj [world x y z])
  (spit @data-file (json/write-str @machine-list)))

(defn count-all-item [inv]
  (reduce #(if %2
             (+ %1 (.getAmount %2))
             %1)
          0 (.getContents inv)))

(defn machine-loop []
  (doseq [[world x y z] @machine-list]
    (let [block (.getBlock (Location. (Bukkit/getWorld world) x y z))]
      (if-let [machine (is-machine block)]
        (when (.isLoaded (.getChunk (machine :core)))
          (let [t-inv (.getInventory (machine :t-hopper))
                b-inv (.getInventory (machine :b-hopper))
                sign (machine :sign)
                n (Integer. (re-find #"[0-9]+" (.getLine (machine :sign) 2)))
                nn (count-all-item t-inv)
                rn (+ n nn)]
            (when (or (not= nn 0) (>= rn 512))
              (.clear t-inv)
              (.update (machine :t-hopper))
              (if (>= rn 512)
                (let [could-not-store-items (.addItem b-inv (into-array [(ItemStack. Material/DIAMOND)]))]
                  (if (.isEmpty could-not-store-items)
                    (do (.update (machine :b-hopper))
                        (.setLine sign 2 (str ChatColor/ITALIC ChatColor/RED
                                               (- rn 512) ChatColor/BLACK "/512")))
                    (.setLine sign 2 (str ChatColor/ITALIC ChatColor/RED
                                          rn ChatColor/BLACK "/512"))))
                (.setLine sign 2 (str ChatColor/ITALIC ChatColor/RED
                                       rn ChatColor/BLACK "/512")))
              (.update (machine :sign)))))
        ; machine is bad
        (del-machine world x y z)))))

(defn third [seq]
  (nth seq 2))

(defn -onEnable [this]
  (reset! logger (.getLogger this))
  (let [cfg-dir (.getDataFolder this)
        d-file (File. cfg-dir "data.json")]
    (reset! data-file (.getPath d-file))
    (if-not (.exists d-file)
      (do (.mkdirs cfg-dir)
          (.createNewFile d-file))
      (let [data (try (json/read-str (slurp @data-file))
                      (catch Exception e []))]
        (doseq [machine data]
          (if (and (= (count machine) 4)
                   (= (class (first machine)) java.lang.String)
                   (= (class (second machine)) java.lang.Long)
                   (= (class (third machine)) java.lang.Long)
                   (= (class (last machine)) java.lang.Long))
            (swap! machine-list conj machine)
            (log-waring (format "Wrong data: %s %s"
                                @data-file machine)))))))
  (-> this
      .getServer
      .getPluginManager
      (.registerEvents this this))
  (.scheduleSyncRepeatingTask (Bukkit/getScheduler) this machine-loop 1 1))

(defn -onSignChange [this event]
  (let [block (.getBlock event)
        machine (is-machine block)]
    (when machine
      (.setLine event 0 (str ChatColor/BOLD ChatColor/BLACK
                             "================="))
      (.setLine event 1 (str ChatColor/BOLD ChatColor/BLUE
                             "[Diamond Machine]"))
      (.setLine event 2 (str ChatColor/ITALIC ChatColor/RED
                             "0" ChatColor/BLACK "/512"))
      (.setLine event 3 (str ChatColor/BOLD ChatColor/BLACK
                             "================="))
      (add-machine (.getName (.getWorld block))
                   (.getX block) (.getY block) (.getZ block)))))
