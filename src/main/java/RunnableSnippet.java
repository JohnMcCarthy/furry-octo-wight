
import com.cloudmine.api.rest.JsonUtilities;
import com.cloudmine.coderunner.SnippetArguments;
import com.cloudmine.coderunner.SnippetContainer;
import com.cloudmine.coderunner.SnippetResponseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <br>
 * Copyright CloudMine LLC. All rights reserved<br>
 * See LICENSE file included with SDK for details.
 */
public class RunnableSnippet implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(RunnableSnippet.class);
    private final SnippetContainer toRun;
    private final SnippetArguments arguments;
    private final HttpServletResponse response;
    public RunnableSnippet(SnippetContainer toRun, SnippetArguments arguments) {
        this(toRun, arguments, null);
    }

    public RunnableSnippet(SnippetContainer toRun, SnippetArguments arguments, HttpServletResponse response) {
        this.toRun = toRun;
        this.arguments = arguments;
        this.response = response;
    }

    @Override
    public void run() {
        Object snippetResponse = toRun.runSnippet(arguments);
        if(response != null) {
            LOG.info("Non null response, returning a result");
            SnippetResponseConfiguration responseConfig = new SnippetResponseConfiguration();
            response.setContentType(responseConfig.getMimeType());
            try {
                JsonUtilities.writeObjectToJson(snippetResponse, response.getOutputStream());
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error("Exception thrown while writing response to json", e);
            }
        }

    }
}
