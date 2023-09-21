/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jochenr.integration.contact;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.LaunchMode;

public class BaseTest {


    protected String getServerHttpUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST)
                ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        final Optional<String> optionalHost = config.getOptionalValue("quarkus.http.test-host", String.class);
        final String host = optionalHost.orElse("localhost");
        System.out.println("Host to call for Test:\t" + host);
        return String.format("http://%s:%d", host, port);
    }

    protected String getServerHttpsUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST)
                ? config.getValue("quarkus.http.test-ssl-port", Integer.class)
                : config.getValue("quarkus.http.ssl-port", Integer.class);
        final Optional<String> optionalHost = LaunchMode.current().equals(LaunchMode.TEST)
                ? config.getOptionalValue("quarkus.http.test-ssl-host", String.class)
                : config.getOptionalValue("quarkus.http.ssl-host", String.class);
        final String host = optionalHost.orElse("localhost");
        System.out.println("Host to call for Test:\t" + host);
        return String.format("https://%s:%d", host, port);
    }

}
