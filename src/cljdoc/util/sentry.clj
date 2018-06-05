(ns cljdoc.util.sentry
  (:require [cljdoc.config :as cfg]
            [raven-clj.core :as raven]
            [raven-clj.interfaces :as interfaces]
            [clojure.tools.logging :as log]))

(defn capture [{:keys [req ex]}]
  (if (cfg/sentry-dsn)
    (let [payload (cond-> {:release (cfg/version)}
                             ex  (interfaces/stacktrace ex ["cljdoc"])
                             req (interfaces/http req identity))
          sentry-response (raven/capture (cfg/sentry-dsn) payload)]
      (when-not (= 200 (:status sentry-response))
        (log/errorf "Failed to log error to Sentry %s %s"
                    (:status sentry-response) (:body sentry-response))))
    (log/warn "DSN missing: Not tracking error in Sentry")))

(def interceptor
  {:name  ::interceptor
   :error (fn sentry-intercept [ctx ex-info]
            (capture {:ex ex-info :req (:request ctx)})
            (throw ex-info))})

(comment
  (raven/parse-dsn dsn)

  (capture {:ex cljdoc.server.pedestal/-error-ex
            :req (:request cljdoc.server.pedestal/-error-ctx)})


  (clojure.pprint/pprint
   (-> {}
       (interfaces/stacktrace cljdoc.server.pedestal/-error-ex)
       (interfaces/http (:request cljdoc.server.pedestal/-error-ctx)
                        identity)))

  (identity cljdoc.server.pedestal/-error-ex)

  )


