

import com.cloudmine.api.rest.JsonUtilities;
import com.cloudmine.coderunner.SnippetContainer;
import org.reflections.Reflections;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <br>
 * Copyright CloudMine LLC. All rights reserved<br>
 * See LICENSE file included with SDK for details.
 */
public class CodeSnippetNameServlet extends HttpServlet{


    private static final Map<String, SnippetContainer> snippetNamesToContainers = new HashMap<String, SnippetContainer>();
    static {
        Reflections reflections = new Reflections("com", "net", "org", "me", "io", "edu", "gov", "mil");
        Set<Class<? extends SnippetContainer>> subTypesOf = reflections.getSubTypesOf(SnippetContainer.class);


        for (Class<? extends SnippetContainer> containerClass : subTypesOf) {
            try {
                SnippetContainer container = containerClass.newInstance();
                String snippetName = container.getSnippetName();

                snippetNamesToContainers.put(snippetName, container);
            } catch (Exception e) {

            }
        }
    }

    public static Map<String, SnippetContainer> getSnippetNamesToContainers() {
        return snippetNamesToContainers;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] snippetNames = snippetNamesToContainers.keySet().toArray(new String[snippetNamesToContainers.size()]);
        JsonUtilities.writeObjectToJson(snippetNames, resp.getOutputStream());
        resp.flushBuffer();
    }
}
