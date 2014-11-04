
import com.cloudmine.api.SimpleCMObject;
import com.cloudmine.api.Strings;
import com.cloudmine.api.rest.JsonUtilities;
import com.cloudmine.coderunner.SnippetArguments;
import com.cloudmine.coderunner.SnippetContainer;
import com.cloudmine.coderunner.SnippetResponseConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class CodeRunnerRootServlet extends HttpServlet {
    private static final long serialVersionUID = 771578936675722864L;
    public static final String VERSION_HEADER_KEY = "X-CloudMine-CodeRunner-Version";
    public static final String DEFAULT_VERSION = "1";
    private static final int BUFFER_SIZE = 4 * 1024;

    public static String inputStreamToString(InputStream inputStream)
            throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(inputStream);
        char[] buffer = new char[BUFFER_SIZE];
        int length;
        while ((length = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, length);
        }
        return builder.toString();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String requestURI = req.getRequestURI();
            int lastSlash = requestURI.lastIndexOf("/");
            if(lastSlash < 0) lastSlash = 0;
            else lastSlash++;
            String snippetName = requestURI.substring(lastSlash); // remove the first slash
            String body = inputStreamToString(req.getInputStream());
            System.out.println("Body input: " + body);
            @SuppressWarnings("unchecked") Map<String, String[]> parameterMap = req.getParameterMap(); // pass the parameter map along to the snippet

            Map<String, SnippetContainer> snippetContainers = CodeSnippetNameServlet.getSnippetNamesToContainers();

            // Check if the snippet container is available based on the path, and activate it if so.
            // Otherwise render a 404 and stop.
            if (snippetContainers.containsKey(snippetName)) {

                String versionString = req.getHeader(VERSION_HEADER_KEY);
                System.out.println("Version string: " + versionString);
                versionString = Strings.isEmpty(versionString) ? DEFAULT_VERSION : versionString;
                int version = 1;
                try {
                    version = Integer.valueOf(versionString);
                }catch(NumberFormatException nfe) {
                }
                SnippetArguments arguments;
                boolean isAsync = false;

                System.out.println("Running version: " + version);

                if(version <= 1) {
                    Map<String, String> convertedParamMap = convertParameterMap(parameterMap);
                    String asyncString = convertedParamMap.get("async");
                    isAsync = asyncString != null && Boolean.parseBoolean(asyncString);
                    arguments = new SnippetArguments(new SnippetResponseConfiguration(), convertedParamMap);
                } else {

                    arguments = new SnippetArguments(new SnippetResponseConfiguration(), body);
                }
                SnippetContainer container = snippetContainers.get(snippetName);
                if(isAsync) {
                    RunnableSnippet snippet = new RunnableSnippet(container, arguments);
                    new Thread(snippet).start();
                } else {
                    RunnableSnippet snippet = new RunnableSnippet(container, arguments, resp);
                    snippet.run();
                }

            } else {
                JsonUtilities.writeObjectToJson("got " + snippetName, resp.getOutputStream());
                resp.flushBuffer();

                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                resp.flushBuffer();
            }
        }catch(Throwable throwable) {
            throwable.printStackTrace();
            SimpleCMObject errorObject = new SimpleCMObject(false);
            errorObject.add("error", throwable.getMessage());
            JsonUtilities.writeObjectToJson(errorObject, resp.getOutputStream());
            resp.flushBuffer();
        }
    }

    private Map<String, String> convertParameterMap(Map<String, String[]> parameterMap) {
        System.out.println("Converting parameter map");
        Map<String, String> convertedParamMap = new HashMap<String, String>();
        for(String key : parameterMap.keySet()) {
            String[] values = parameterMap.get(key);
            String valueAsString = getValueAsString(values);
            String decoded;
            try {
                decoded = URLDecoder.decode(valueAsString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                decoded = URLDecoder.decode(valueAsString);
            }
            System.out.println("Decoded to: " + decoded);
            convertedParamMap.put(key, decoded);
        }
        return convertedParamMap;
    }

    private String getValueAsString(String[] values) {
        if(values == null) return "";
        switch(values.length) {
            case 0:
                return "";
            case 1:
                return values[0];
            default:
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < values.length; i++) {
                    builder.append(values[i]);
                }
                return builder.toString();
        }
    }

    public static void main(String[] args) throws Exception{
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new CodeSnippetNameServlet()), "/names");
        context.addServlet(new ServletHolder(new CodeRunnerRootServlet()),"/code/*");
        context.addServlet(new ServletHolder(new HelloWorld()), "/hello");
        server.start();
        server.join();
    }
}
