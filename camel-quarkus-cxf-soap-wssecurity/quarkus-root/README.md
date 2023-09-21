

mvn clean install -Dquarkus.http.test-ssl-host=localhost

mvn verify -Dquarkus.http.test-ssl-host=localhost

mvn verify -Dquarkus.http.test-ssl-host=[your real hostname]
