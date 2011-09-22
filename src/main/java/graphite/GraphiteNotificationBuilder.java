package graphite;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Locale;

/**i.i 8.0 1316723639
 */
public class GraphiteNotificationBuilder extends Builder {

    private final String path;
    private final String host;
	private final int port;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public GraphiteNotificationBuilder(String path, String host, int port) {
        this.path = path;
        this.port = port;
    	this.host = host;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getPath() {
        return path;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        Socket socket = null;
        Writer writer=null;
        try {
            InetSocketAddress sockaddr = new InetSocketAddress(host, port);
            socket = new Socket();
            socket.connect(sockaddr,10000);
            writer = new OutputStreamWriter(socket.getOutputStream());
            long epoch = System.currentTimeMillis() / 1000;


            String date =epoch+"";

            String metric  = String.format(Locale.US, "%s %s %s\n", path, (build.getDuration()+1)+"", date);
			listener.getLogger().println("sending:" + metric);
            writer.write(metric);

            writer.flush();
        } catch (Exception e) {
			listener.getLogger().println(e);	
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                }
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
            writer = null;
        }
        
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a hostname");
            return FormValidation.ok();
        }
        
    	public FormValidation doCheckPort(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a port");
            return FormValidation.ok();
        }
        
        public FormValidation doCheckPath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Graphite Notification";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }
}

