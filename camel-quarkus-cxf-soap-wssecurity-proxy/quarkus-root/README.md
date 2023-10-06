

mvn clean install -DskipITs=true -Dskip.surefire.tests=true



mvn verify -Dtest=CxfClientSyncTest#testGetContact

mvn verify -Dquarkus.http.test-ssl-host=localhost -Dtest=CxfClientSyncTest#testGetContactDispatchMode





mvn verify -Dquarkus.http.test-ssl-host=[your real hostname]
