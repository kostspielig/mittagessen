;; Copyright (c) 2016 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
;;
;; This file is part of Mittagessen.
;;
;; Mittagessen is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; Mittagessen is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Mittagessen.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns mittagessen.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]))

(defonce app-state
  (r/atom {:data nil
           :choice nil
           :options #{:disabled}
           :animation {:duration 0
                       :angle 0
                       :choosing false}}))

(def css-transitions
  (r/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn rand-nth-bucket [elems bucket]
  (let [total (reduce + (map bucket elems))
        n     (rand total)
        step
        (fn [[acc last] elem]
          (if (< n acc)
            (reduced [acc last])
            [(+ acc (bucket elem)) elem]))]
    (second (reduce step [0 nil] elems))))

(defn options-view [state]
  (r/with-let
    [visible (r/track #(not (:disabled @state)))

     toggle-option!
     (fn [tag] (swap! state #((if (tag %) disj conj) % tag)))

     option-view
     (fn [state tag name]
       (r/with-let [toggled (r/track #(tag @state))]
         [:button.option.clickable {:class (when @toggled "checked")
                                 :on-click #(toggle-option! tag)}
          [:div.check] name]))]
    [:div
     [:div.options {:class (if @visible "visible" "invisible")}
      [:div.columns
       [:div.column
        [option-view state :hurry "I'm in a hurry"]
        [option-view state :chill "Let's chill"]
        [option-view state :hungry "I'm very hungry"]
        [option-view state :healthy "Healthy, please!"]]
       [:div.column
        [option-view state :broke "I'm broke"]
        [option-view state :fancy "Let's go fancy"]
        [option-view state :rainy "It's rainy"]
        [option-view state :sunny "It's sunny"]]]]
     [:div.bottom
      [:button.clickable {:class (when @visible "checked")
                          :on-click #(toggle-option! :disabled)}
       (if @visible "Less options!" "Moar options!")]]]))

(defn game-view [state]
  (r/with-let
    [chance
     (fn [place filters]
       (let [filters (:options @state)
             chance- (:chance place)
             options (:options place)]
         (max 0
              (+ (if (number? chance-) chance- 1)
                 (reduce #(+ %1 (get options %2 0)) 0 filters)))))

     animation (r/cursor state [:animation])

     choose-place!
     (fn []
       (reset! animation {:choosing true
                          :angle 0
                          :duration 0
                          :ease "linear"})
       (swap! state assoc-in [:choice]
              (rand-nth (:data @state)))
       (.requestAnimationFrame
        js/window
        (fn []
          (go
            (doseq [[i n] (map vector (range 11)
                               (concat (range 2 8) (range 7 2 -1)))]
              (reset! animation {:choosing true
                                 :angle (/ (* (+ i 1) 360) 4)
                                 :duration (/ 1000 n)
                                 :ease "linear"})
              (swap! state assoc-in [:choice]
                     (rand-nth (:data @state)))
              (<! (timeout (/ 1000 n))))
            (swap! state assoc-in [:choice]
                   (rand-nth-bucket (:data @state) chance))
            (reset! animation {:choosing false
                               :angle (/ (* 12 360) 4)
                               :duration (/ 1000 1)
                               :ease "ease-out"})))))

     reset-place!
     #(swap! state assoc-in [:choice] nil)]
    [css-transitions {:transition-name "fade"
                      :transition-appear true
                      :transition-appear-timeout 500
                      :transition-enter-timeout 500
                      :transition-leave-timeout 500}
     (if-let [choice (:choice @state)]
       ;; We already have a choice
       ^{:key :result-page}
       [:div.full
        [:div.centered
         {:style
          {:transform  (str "rotate(" (:angle @animation) "deg)")
           :transition (str "transform "
                            (:duration @animation) "ms "
                            (:ease @animation))}}
         [:h1.result (:name choice)]
         (when-not (:choosing @animation)
           [:div.absolute
            [:a {:href (:where choice)}
             "Where is this?"]])]
        (when-not (:choosing @animation)
          [:div.bottom
           [:button.clickable {:on-click reset-place!}
            "I don't like this result!"]])]

       ;; First time question
       ^{:key :ask-page}
       [:div.full
        [:div.centered
         [:h1 "Where should we go for lunch?"]
         [:button.button {:on-click choose-place!} "Choose!"]]
        [options-view (r/cursor state [:options])]])]))


(defn emoji-view [emoji]
  (r/with-let [html-text (.unicodeToImage js/emojione emoji)
               view-impl (constantly [:div {:dangerouslySetInnerHTML
                                            {:__html html-text}}])
               generate (fn []
                          {:translate [(rand (.-innerWidth js/window))
                                       (rand (.-innerHeight js/window))]
                           :scale (+ 0.2 (rand 0.8))
                           :duration (+ 2000 (rand-int 10000))})
               data (r/atom (generate))
               _ (go (loop []
                       (reset! data (generate))
                       (<! (timeout (:duration @data)))
                       (recur)))]
    (let [{[x y] :translate
           scale :scale
           dur   :duration} @data]
      [:div {:style {:position "absolute"
                     :transform (str "translate(" x "px," y "px) "
                                     "scale(" scale ", " scale ")")
                     :transition (str "transform " dur "ms linear")}}
       [view-impl emoji]])))

(defn emojis-view []
  (r/with-let
    [emojis ["🦀""🧀""🌭""🌮""🌯""🍿""🍾""🌶""🐖""🐷""🐔""🍉"
             "🍇""🍊""🍋""🍌""🍓""🍅""🍆""🌽""🍑""🍐""🍎""🍏"
             "🍍""🍈""🍄""🍞""🍖""🍗""🍔""🍟""🍕""🍲""🍱""🍙"
             "🍚""🍛""🍜""🍝""🍠""🍢""🍣""🍤""🍥""🍡""🍦""🍧"
             "🍨""🍩""🍪""🎂""🍰""🍫""🍬""🍭""🍮""🍯""🍼""🍵"
             "🍶""🍷""🍸""🍹""🍺""🍻""🍴""🍳"]]
    [:div.emojis
     (for [emoji emojis]
       ^{:key emoji}
       [emoji-view emoji])]))

(defn root-view [state]
  (go
    (let [data (:body (<! (http/get "data/places.json")))]
      (swap! state assoc-in [:data] data)))
  (fn [state]
    [:div.content
     (when-let [choice (:choice @state)]
       (let [animation (:animation @state)]
         {:style {:background-color (:bg choice)
                  :transition (str "all "
                                   (:duration animation) "ms "
                                   (:ease animation))
                  :color (:fg choice)}}))
     [emojis-view]
     (when (:data @state)
       [game-view state])]))

(defn init-app! []
  (enable-console-print!)
  (prn "Mittagsessen app started!")
  (r/render-component
   [root-view app-state]
   (.getElementById js/document "components")))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! app-state update-in [:__figwheel_counter] inc))

(init-app!)
