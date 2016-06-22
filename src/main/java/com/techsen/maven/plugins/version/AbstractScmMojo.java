package com.techsen.maven.plugins.version;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmRevision;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractScmMojo extends AbstractMojo {

    /**
     * The SCM connection URL.
     */
    @Parameter(property = "connectionUrl", defaultValue = "${project.scm.connection}")
    private String connectionUrl;

    /**
     * The SCM connection URL for developers.
     */
    @Parameter(property = "connectionUrl", defaultValue = "${project.scm.developerConnection}")
    private String developerConnectionUrl;

    /**
     * The type of connection to use (connection or developerConnection).
     */
    @Parameter(property = "connectionType", defaultValue = "connection")
    private String connectionType;

    /**
     * The working directory.
     */
    @Parameter(property = "workingDirectory")
    private File workingDirectory;

    /**
     * Comma separated list of includes file pattern.
     */
    @Parameter(property = "includes")
    private String includes;

    /**
     * Comma separated list of excludes file pattern.
     */
    @Parameter(property = "excludes")
    private String excludes;

    @Component
    private ScmManager manager;

    /**
     * The base directory.
     */
    @Parameter(property = "basedir", required = true)
    private File basedir;

    /**
     * List of System properties to pass to the JUnit tests.
     */
    @Parameter
    private Properties systemProperties;

    /**
     * List of provider implementations.
     */
    @Parameter
    private Map<String, String> providerImplementations;

    /**
     * Should distributed changes be pushed to the central repository? For many
     * distributed SCMs like Git, a change like a commit is only stored in your
     * local copy of the repository. Pushing the change allows your to more
     * easily share it with other users.
     *
     * @since 1.4
     */
    @Parameter(property = "pushChanges", defaultValue = "true")
    private boolean pushChanges;

    /** {@inheritDoc} */
    public void execute() throws MojoExecutionException {
        if (systemProperties != null) {
            // Add all system properties configured by the user
            Iterator<Object> iter = systemProperties.keySet().iterator();

            while (iter.hasNext()) {
                String key = (String) iter.next();

                String value = systemProperties.getProperty(key);

                System.setProperty(key, value);
            }
        }

        if (providerImplementations != null && !providerImplementations.isEmpty()) {
            for (Entry<String, String> entry : providerImplementations.entrySet()) {
                String providerType = entry.getKey();
                String providerImplementation = entry.getValue();
                getLog().info("Change the default '" + providerType + "' provider implementation to '"
                        + providerImplementation + "'.");
                getScmManager().setScmProviderImplementation(providerType, providerImplementation);
            }
        }
    }

    protected void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionUrl() {
        boolean requireDeveloperConnection = !"connection".equals(connectionType.toLowerCase());
        if (StringUtils.isNotEmpty(connectionUrl) && !requireDeveloperConnection) {
            return connectionUrl;
        } else if (StringUtils.isNotEmpty(developerConnectionUrl)) {
            return developerConnectionUrl;
        }
        if (requireDeveloperConnection) {
            throw new NullPointerException("You need to define a developerConnectionUrl parameter");
        } else {
            throw new NullPointerException("You need to define a connectionUrl parameter");
        }
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public File getWorkingDirectory() {
        if (workingDirectory == null) {
            return basedir;
        }

        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public ScmManager getScmManager() {
        return manager;
    }

    public ScmFileSet getFileSet() throws IOException {
        if (includes != null || excludes != null) {
            return new ScmFileSet(getWorkingDirectory(), includes, excludes);
        } else {
            return new ScmFileSet(getWorkingDirectory());
        }
    }

    public ScmRepository getScmRepository() throws ScmException {
        ScmRepository repository;

        try {
            repository = getScmManager().makeScmRepository(getConnectionUrl());

            ScmProviderRepository providerRepo = repository.getProviderRepository();

            providerRepo.setPushChanges(pushChanges);

        } catch (ScmRepositoryException e) {
            if (!e.getValidationMessages().isEmpty()) {
                for (String message : e.getValidationMessages()) {
                    getLog().error(message);
                }
            }

            throw new ScmException("Can't load the scm provider.", e);
        } catch (Exception e) {
            throw new ScmException("Can't load the scm provider.", e);
        }

        return repository;
    }

    public void checkResult(ScmResult result) throws MojoExecutionException {
        if (!result.isSuccess()) {
            getLog().error("Provider message:");

            getLog().error(result.getProviderMessage() == null ? "" : result.getProviderMessage());

            getLog().error("Command output:");

            getLog().error(result.getCommandOutput() == null ? "" : result.getCommandOutput());

            throw new MojoExecutionException(
                    "Command failed." + StringUtils.defaultString(result.getProviderMessage()));
        }
    }

    public String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    public ScmVersion getScmVersion(String versionType, String version) throws MojoExecutionException {
        if (StringUtils.isEmpty(versionType) && StringUtils.isNotEmpty(version)) {
            throw new MojoExecutionException("You must specify the version type.");
        }

        if (StringUtils.isEmpty(version)) {
            return null;
        }

        if ("branch".equals(versionType)) {
            return new ScmBranch(version);
        }

        if ("tag".equals(versionType)) {
            return new ScmTag(version);
        }

        if ("revision".equals(versionType)) {
            return new ScmRevision(version);
        }

        throw new MojoExecutionException("Unknown '" + versionType + "' version type.");
    }

}
