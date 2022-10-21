# quote-generator project

- Qute + Unpoly templating
- Server Side Events
- Reactive Mutiny
- Vert.x event bus
- Jsonb

Compile and run locally
```
mvn quarkus:dev
```

Tests
```
# json
http -j localhost:8080/quotes

# html
http localhost:8080/quotes

# stream
http localhost:8080/quotes/stream --stream
```

Browse to
```bash
http://localhost:8080/quotes
```

<img src="images/quotes.gif" width="400">

Run on OpenShift
```bash
oc new-app quay.io/eformat/quote-generator:latest
oc create route edge quote-generator --service=quote-generator --port=8080
```
