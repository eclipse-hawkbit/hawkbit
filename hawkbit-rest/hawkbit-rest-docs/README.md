# hawkBit Rest Documentation tests

The hawkBit Rest Documentation tests are based on [Rest Docs](https://projects.spring.io/spring-restdocs/). These tests generate documentation for our RESTful services.

## Run and create snippets

Run the test with maven

```bash
mvn clean package
```

Every rest test will create snippets (e.g. curl-request.adoc, http-request.adoc) in the target\generated-snippets\ directory.

## Use the snippets

The snippets get included using Asciidoc within our API documents in src\main\asciidoc. Those documents in turn are used to generate HTML documents in the target\classes directory when building with maven.
