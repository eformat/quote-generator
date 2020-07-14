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
# Browse to
http://localhost:8080/quotes

# json
http -j localhost:8080/quotes

# html
http localhost:8080/quotes

# stream
http localhost:8080/quotes/stream --stream
```

