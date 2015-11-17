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

import org.gephi.maven.json.PluginsMetadata;
import org.gephi.maven.json.PluginMetadata;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.gephi.maven.json.Version;

/**
 * Builds the plugins metadata.
 */
@Mojo(name = "build-metadata", aggregator = true, defaultPhase = LifecyclePhase.SITE)
public class BuildMetadata extends AbstractMojo {

    /**
     * Output directory.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/site")
    protected File outputDirectory;

    /**
     * NBM manifest path.
     */
    @Parameter(required = true, defaultValue = "src/main/nbm/manifest.mf")
    private String sourceManifestFile;

    /**
     * If the executed project is a reactor project, this will contains the full
     * list of projects in the reactor.
     */
    @Parameter(required = true, readonly = true, property = "reactorProjects")
    private List<MavenProject> reactorProjects;

    /**
     * The Maven project.
     */
    @Parameter(required = true, readonly = true, property = "project")
    private MavenProject project;

    /**
     * Data format for <em>lastUpdated</em> field.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String gephiVersion = (String) project.getProperties().get("gephi.version");
        if (gephiVersion == null) {
            throw new MojoExecutionException("The 'gephi.version' property should be defined");
        }
        getLog().debug("Building metadata for 'gephi.version=" + gephiVersion + "'");

        if (reactorProjects != null && reactorProjects.size() > 0) {
            getLog().debug("Found " + reactorProjects.size() + " projects in reactor");
            List<MavenProject> modules = new ArrayList<MavenProject>();
            for (MavenProject proj : reactorProjects) {
                if (proj.getPackaging().equals("nbm")) {
                    String gephiVersionModule = proj.getProperties().getProperty("gephi.version");

                    if (gephiVersionModule.equals(gephiVersion)) {
                        getLog().debug("Found 'nbm' project '" + proj.getName() + "' with artifactId=" + proj.getArtifactId() + " and groupId=" + proj.getGroupId());
                        modules.add(proj);
                    } else {
                        getLog().debug("Ignored project '" + proj.getName() + "' based on 'gephi.version' value '" + gephiVersionModule + "'");
                    }
                }
            }

            ManifestUtils manifestUtils = new ManifestUtils(sourceManifestFile, getLog());

            // Get all modules with dependencies
            Map<MavenProject, List<MavenProject>> tree = ModuleUtils.getModulesTree(modules, getLog());

            // Build metadata
            PluginsMetadata pluginsMetadata = new PluginsMetadata();
            List<PluginMetadata> pluginMetadatas = new ArrayList<PluginMetadata>();
            for (Map.Entry<MavenProject, List<MavenProject>> entry : tree.entrySet()) {
                MavenProject topPlugin = entry.getKey();
                PluginMetadata pm = new PluginMetadata();
                pm.id = topPlugin.getGroupId() + "." + topPlugin.getArtifactId();
                manifestUtils.readManifestMetadata(topPlugin, pm);
                pm.license = MetadataUtils.getLicenseName(topPlugin);
                pm.authors = MetadataUtils.getAuthors(topPlugin);
                pm.last_update = dateFormat.format(new Date());
                pm.readme = MetadataUtils.getReadme(topPlugin, getLog());
                pm.images = ScreenshotUtils.copyScreenshots(topPlugin, new File(outputDirectory, "imgs" + File.separator + pm.id), "imgs" + File.separator + pm.id + "/", getLog());

                if (pm.versions == null) {
                    pm.versions = new HashMap<String, Version>();
                }
                Version v = new Version();
                v.last_update = dateFormat.format(new Date());
                v.url = gephiVersion + "/" + ModuleUtils.getModuleDownloadPath(entry.getKey(), entry.getValue(), new File(outputDirectory, gephiVersion), getLog());
                pm.versions.put(gephiVersion, v);

                pluginMetadatas.add(pm);
            }
            pluginsMetadata.plugins = pluginMetadatas;

            // Build json file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(pluginsMetadata);

            // Write json file
            try {
                FileWriter writer = new FileWriter(new File(outputDirectory, "plugins.json"));
                writer.append(json);
                writer.close();
            } catch (IOException ex) {
                throw new MojoExecutionException("Error while writing plugins.json file", ex);
            }
        } else {
            throw new MojoExecutionException("The project should be a reactor project");
        }
    }
}
