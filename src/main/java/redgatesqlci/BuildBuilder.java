package redgatesqlci;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class BuildBuilder extends Builder {

    private String dbFolder;
    public String getDbFolder() {
        return dbFolder;
    }

    private String subfolder;
    public String getSubfolder() {
        return subfolder;
    }

    private final String packageid;
    public String getPackageid() {
        return packageid;
    }

    private final String tempServer;
    public String getTempServer() {
        return tempServer;
    }

    private final String serverName;
    public String getServerName() {
        return serverName;
    }

    private final String dbName;
    public String getDbName() {
        return dbName;
    }

    private final String serverAuth;
    public String getServerAuth() {
        return serverAuth;
    }

    private final String username;
    public String getUsername() {
        return username;
    }

    private final String password;
    public String getPassword() {
        return password;
    }

    private final String additionalParams;
    public String getAdditionalParams() {
        return additionalParams;
    }

    @DataBoundConstructor
    public BuildBuilder(DbFolder dbFolder, String packageid, Server tempServer, String additionalParams) {
        this.dbFolder = dbFolder.getvalue();
        this.subfolder = dbFolder.getsubfolder();
        this.packageid = packageid;
        this.tempServer = tempServer.getvalue();

        if(this.tempServer.equals("sqlServer")) {
            this.dbName = tempServer.getDbName();
            this.serverName = tempServer.getServerName();
            this.serverAuth = tempServer.getServerAuth().getvalue();
            this.username = tempServer.getServerAuth().getUsername();
            this.password = tempServer.getServerAuth().getPassword();
        }
        else
        {
            this.dbName = "";
            this.serverName = "";
            this.serverAuth = "";
            this.username = "";
            this.password = "";
        }

        this.additionalParams = additionalParams;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        ArrayList<String> params = new ArrayList<String>();

        FilePath checkOutPath = build.getWorkspace();
        params.add("BUILD");

        if (getDbFolder().equals("subfolder")) {
            params.add("/scriptsFolder=" + checkOutPath.getRemote()  + getSubfolder());
        } else{
            params.add("/scriptsFolder=" + checkOutPath.getRemote());
        }
        params.add("/packageId=" + getPackageid());
        params.add("/packageVersion=0." + build.getNumber());

        if (!additionalParams.isEmpty())
            params.add("/additionalCompareArgs=\"" + getAdditionalParams() + "\"");

        if (getTempServer().equals("sqlServer")) {
            params.add("/temporaryDatabaseServer=" + getServerName());
            params.add("/temporaryDatabaseName=" + getDbName());

            if (getServerAuth().equals("sqlServerAuth")) {
                params.add("/temporaryDatabaseUserName=" + getUsername());
                params.add("/temporaryDatabasePassword=" + getPassword());
            }
        }

        return Utils.runSQLCIWithParams(build, launcher, listener, params);
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link BuildBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckPackageid(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Enter a package ID.");
            return FormValidation.ok();
        }

        // Since the AJAX callbacks don't give the value of radioblocks, I can't validate the value of the server and
        // database name fields.

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Redgate SQL CI: Build a database package";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req,formData);
        }
    }
}

