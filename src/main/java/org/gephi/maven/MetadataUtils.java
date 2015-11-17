/*
 * Copyright 2015 Gephi Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gephi.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.gephi.maven.json.Author;

/**
 * Metadata utils.
 */
public class MetadataUtils {

    /**
     * Lookup and returns the value of the <em>licenseName</em> configuration.
     *
     * @param project project
     * @return license name or null if not found
     */
    protected static String getLicenseName(MavenProject project) {
        Plugin nbmPlugin = lookupNbmPlugin(project);
        if (nbmPlugin != null) {
            Xpp3Dom config = (Xpp3Dom) nbmPlugin.getConfiguration();
            if (config != null && config.getChild("licenseName") != null) {
                return config.getChild("licenseName").getValue();
            }
        }
        return null;
    }

    /**
     * Lookup and returns the value of the <em>author</em> configuration.
     * <p>
     * The configuration string is split based on ',' so multiple authors can be
     * defined.
     *
     * @param project project
     * @return list of authors or null if not found
     */
    protected static List<Author> getAuthors(MavenProject project) {
        Plugin nbmPlugin = lookupNbmPlugin(project);
        if (nbmPlugin != null) {
            Xpp3Dom config = (Xpp3Dom) nbmPlugin.getConfiguration();
            if (config != null && config.getChild("author") != null) {
                String authors = config.getChild("author").getValue();
                List<Author> res = new ArrayList<Author>();
                for (String a : authors.split(",")) {
                    Author author = new Author();
                    author.name = a.trim();
                    res.add(author);
                }
                return res;
            }
        }
        return null;
    }

    /**
     * Lookup and return the NBM plugin for this plugin.
     *
     * @param project project
     * @return NBM plugin
     */
    protected static Plugin lookupNbmPlugin(MavenProject project) {
        List plugins = project.getBuildPlugins();

        for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
            Plugin plugin = (Plugin) iterator.next();
            if ("org.codehaus.mojo:nbm-maven-plugin".equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Lookup and return the content of the README.md file for this plugin.
     *
     * @param project project
     * @param log log
     * @return content of REDME.md file or null if not found
     */
    protected static String getReadme(MavenProject project, Log log) {
        File readmePath = new File(project.getBasedir(), "README.md");
        if (readmePath.exists()) {
            log.debug("README.md file has been found: '" + readmePath.getAbsolutePath() + "'");
            try {
                StringBuilder builder = new StringBuilder();
                LineNumberReader fileReader = new LineNumberReader(new FileReader(readmePath));
                String line;
                while ((line = fileReader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                }
                log.info("File README.md with " + builder.length() + " characters has been attached to project '" + project.getName() + "'");
                return builder.toString();
            } catch (IOException ex) {
                log.error("Error while reading README.md file", ex);
            }
        }
        return null;
    }
}
