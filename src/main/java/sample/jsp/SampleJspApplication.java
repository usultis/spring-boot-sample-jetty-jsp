/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jsp;

import com.google.common.collect.ObjectArrays;
import com.google.common.io.Files;
import org.apache.jasper.runtime.TldScanner;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@SpringBootApplication
public class SampleJspApplication extends SpringBootServletInitializer {

    private static final Pattern jspEntryMatcher = Pattern.compile(".*[.](jsp|tag)");

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SampleJspApplication.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleJspApplication.class, args);
    }

    @Bean
    public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
        clearJspSystemUris();
        JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory = new JettyEmbeddedServletContainerFactory() {
            @Override
            protected void postProcessWebAppContext(WebAppContext webAppContext) {
                super.postProcessWebAppContext(webAppContext);
                Optional<String> jarFile = jarFile();
                if (jarFile.isPresent()) {
                    File tempDir = Files.createTempDir();
                    webAppContext.setTempDirectory(tempDir);
                    webAppContext.setContextPath("/");
                    webAppContext.setPersistTempDirectory(true);
                    webAppContext.setConfigurations(ObjectArrays.concat(webAppContext.getConfigurations(), new WebInfConfiguration()));
                    new Unzipper().unzip(jarFile.get(), tempDir, jspZipEntry());
                    logger.info("JSP files extracted to directory: " + tempDir.getAbsolutePath());
                    webAppContext.setWar(tempDir.getPath());
                } else {
                    webAppContext.setWar(this.getClass().getResource("/").getPath());
                }
            }

            private Predicate<String> jspZipEntry() {
                return new Predicate<String>() {
                    @Override
                    public boolean test(String zipEntry) {
                        return jspEntryMatcher.matcher(zipEntry).matches();
                    }
                };
            }

            private Optional<String> jarFile() {
                try {
                    return Optional.of(new URL(substringBefore(this.getClass().getResource("").getPath(), "!")).getFile());
                } catch (MalformedURLException e) {
                    return Optional.empty();
                }
            }

            private String substringBefore(String string, String delimiter) {
                int delimiterIndex = string.indexOf(delimiter);
                return delimiterIndex != -1 ? string.substring(0, delimiterIndex) : null;
            }

        };
        jettyEmbeddedServletContainerFactory.addServerCustomizers(new JettyServerCustomizer() {
            @Override
            public void customize(Server server) {
                org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
                classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

            }
        });
        return jettyEmbeddedServletContainerFactory;
    }

    private void clearJspSystemUris() {
        Field systemUris = ReflectionUtils.findField(TldScanner.class, "systemUris");
        systemUris.setAccessible(true);
        ReflectionUtils.setField(systemUris, null, new HashSet<String>());
    }

}
