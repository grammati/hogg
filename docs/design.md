# Stuff

Thoughts on building an async proxy / API layer.

Stuff to handle:

- Plugable proxying
  - Simple interface to mount a service at a path
  - per-mount-point configuration of security, rate-limiting, ...
  - or maybe even more granular? (why not?)
- Can proxy http[s?], websockets, ...
  - Terminate SSL?
  - websockets: proxy or redirect?
    - if proxy, can log, track metrics, etc.
- Service discovery
- Pinning
- Smart load-balancing
- Smart retry
- Caching?
- Logging / auditing
- Circuit-breaking (aka: fallbacks for unavailable services)
- Metrics
  - build in lots of visibility to internals (make Chippy smile)
  

Maybe:

- Aggregation
  - Allow some endpoints to be handled in-process
  - Call to and aggregate results of calls to backend services
  - Probably only for when calling to at least 2 different services
  - Framework for really easy parallelization of requests (dataflow)
- Sessions
  - Session-state (stored in bagboy/datomic?)
- Authorizations
  - Cache of auth info


Reasons why this is a terrible idea:

- Dude, just use nginx
  - But I want to be able to write my logic in Clojure
- The JVM is unsuitable for a high-performance proxy - GC pauses suck
  - Shrug. I think I'll try it anyway and see how it pans out.
  

## Questions, Ideas, Whatever

Expose http-kit as core.async channels? Use core.async as the common
abstraction for everything (client, server, whatever).

Async-ring. Play with munging http-kit channels, core.async, and some
sort of Pedestal-inspired generic routing/middleware framework that
supports websockets too.

Think about horizontal scalability.

