(defproject mittagessen "0.1.0-SNAPSHOT"
  :description "Web user interface for the Mittagessen app"
  :url "http://arximboldi.github.com/mittagessen"

  :dependencies [[cljs-http "0.1.39"]
                 [cljsjs/react-with-addons "0.14.3-0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [reagent "0.6.0-alpha" :exclusions [cljsjs/react]]]

  :min-lein-version "2.5.3"

  :plugins [[lein-figwheel "0.5.0-1"]
            [lein-cljsbuild "1.1.2"]]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/js/debug" "target"]

  :cljsbuild {
    :builds [
      {:id "debug"
       :source-paths ["src"]
       :figwheel {:on-jsload "mittagessen.core/on-figwheel-reload!"}
       :compiler {:asset-path "js/debug/out"
                  :output-to "resources/js/debug/mittagessen.js"
                  :output-dir "resources/js/debug/out"
                  :main mittagessen.core
                  :optimizations :none
                  :source-map-timestamp true}}
      {:id "release"
       :source-paths ["src"]
       :compiler {:asset-path "js/release/out"
                  :output-to "resources/js/release/mittagessen.js"
                  :output-dir "resources/js/release/out"
                  :main mittagessen.core
                  :optimizations :advanced
                  :pretty-print false}}]}

  :figwheel {:http-server-root ""
             :css-dirs ["resources/css"]})
