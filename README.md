# Example of a service, HTTP endpoints and two interpreters (tapir + ZIO2)

This repository contains a small sample of a:

 * service that does some CRUD operations on a foobar (to make it sufficiently abstract)
 * some HTTP endpoint definitions for exposing it (using [tapir])
 * a HTTP server backend using [http4s] in `./service-http4s/`
 * an abstract message-based backend in `./service-message-based/`

The latter was the point of this sample project - to demonstrate the feasibility of hooking
up HTTP server semantics to a message-based "RPC-like" backend, while keeping all the HTTP semantics
and not having to rewrite any code in the main service. See the 
[blog post](https://blog.solidninja.is/posts/2022-http-services-behind-message-gateways/) for more details.

[http4s]: https://http4s.org/
[tapir]: https://tapir.softwaremill.com
[zio]: https://zio.dev
