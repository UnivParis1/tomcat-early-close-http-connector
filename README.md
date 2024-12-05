# Zero downtime Tomcat restart using plain Docker

With Docker, you can easily do things like:

  * rename container
```
docker container rename foo old-foo
```
  * tell tomcat to shutdown ASAP
```
docker kill --signal TERM old-foo
```
  * start new tomcat
```
docker run --network=host ...
```

This works easily for PHP-FPM. But what about Tomcat?

Related pages: https://iximiuz.com/en/posts/multiple-containers-same-port-reverse-proxy/ https://innovation.ebayinc.com/tech/engineering/zero-downtime-instant-deployment-and-rollback/


## Issues to solve

### Tomcat default behaviour

- on startup: 
    - bind HTTP port
    - start webapps
- on shutdown: 
    - handle remaining requests, it waits for "[gracefulStopAwaitMillis](https://tomcat.apache.org/tomcat-9.0-doc/config/service.html#Attributes_Standard%20Implementation_gracefulStopAwaitMillis)" / "[unloadDelay](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html#Attributes_Standard%20Implementation_unloadDelay)" (only 2 seconds by default)
    - save sessions
    - unbind HTTP port

Issues: 
- during "gracefulStopAwaitMillis", new requests are waiting and will be aborted


### With Connector `bindOnInit="false"` ([doc](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html#Attributes_Standard%20Implementation_bindOnInit))

- on startup: 
    - start webapps
    - bind HTTP port
- on shutdown: 
    - unbind HTTP port
    - handle remaining requests
    - save sessions

Issues:
- when the second tomcat is starting, the sessions file may not be available yet
- during the second tomcat startup, the requests will return an error (HTTP 502 if rev proxied). This is especially bad for POST like requests


### With EarlyCloseHttp11NioProtocol and EarlySessionsUnloadManager:

- on startup: 
    - bind HTTP port
    - start webapps
- on shutdown: 
    - save sessions
    - unbind HTTP port
    - handle remaining requests

Issues: none!

An alternative to EarlyCloseHttp11NioProtocol is to use [unixDomainSocketPath](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html#Unix_Domain_Socket_Support) (if you have a apache2/nginx on the same host) since you can easily `rm` it.


## Usage

### EarlyCloseHttp11NioProtocol

```
<Connector protocol="HTTP/1.1" ...
```
with

```
<Connector protocol="fr.univparis1.tomcat.EarlyCloseHttp11NioProtocol" ...
```

### EarlySessionsUnloadManager

```
<Context>
    <Manager className="fr.univparis1.tomcat.EarlySessionsUnloadManager" pathname="/var/lib/sessions/" />
</Context>
```

It requires a listener:

```
<Server port="-1">
  <Listener className="fr.univparis1.tomcat.EarlySessionsUnloadListener" />
  ...
```

Compared to StandardManager, it features storing sessions in per-webapp files when "pathname" is a directory.