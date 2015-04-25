package ${packageName};

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;


public class AbstractServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AbstractServlet.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss");
    protected static final DateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        logger.trace(pathInfo);

        try {
            Map<String, Object> model = new HashMap<>();

            String viewName = handleGetRequest(request, response, pathInfo, model);
            if (viewName != null) {
                char c = viewName.charAt(0);
                if (c == '{') {
                    renderJsonResponse(response, viewName);
                } else if (c == '<') {
                    renderXmlResponse(response, viewName);
                } else {
                    renderResponse(request, response, viewName, model);
                }
            }
        } catch (HttpResponseException e) {
            e.sendError(response);
        }
    }

//    @Override
//    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doHead(req, resp);
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);
//    }
//
//    @Override
//    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPut(req, resp);
//    }
//
//    @Override
//    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doDelete(req, resp);
//    }

    /**
     * Must be implemented by subclasses which handle GET requests without a custom <code>doGet()</code>.
     * Called by {@link #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     *
     * @param request
     * @param response
     * @param pathInfo
     * @param model
     * @return A JSON or XML response body or the name of the view to pass to {@link #renderResponse}.
     *
     * @throws HttpResponseException
     * @throws IOException
     */
    protected String handleGetRequest(HttpServletRequest request, HttpServletResponse response,
                                    String pathInfo, Map<String, Object> model) throws HttpResponseException, IOException {
        throw new HttpResponseException(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    protected final void renderJsonResponse(HttpServletResponse response, Map<String, Object> model) throws IOException {
        String json = jsonMapper.writeValueAsString(model);
        renderJsonResponse(response, json);
    }

    protected final void renderJsonResponse(HttpServletResponse response, String json) throws IOException {
        response.getWriter().write(json);
    }

    protected final void renderXmlResponse(HttpServletResponse response, String xml) throws IOException {
        response.getWriter().write(xml);
    }

    protected final void renderResponse(HttpServletRequest request, HttpServletResponse response, String viewName) throws ServletException, IOException {
        renderResponse(request, response, viewName, null);
    }

    /**
     * Called by the default {@link #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     *
     * @param request
     * @param response
     * @param viewName - name of JSP file in /WEB-INF/views/ (without the .jsp suffix)
     * @param model
     * @throws ServletException
     * @throws IOException
     */
    protected final void renderResponse(HttpServletRequest request, HttpServletResponse response,
                                         String viewName, Map<String, Object> model) throws ServletException, IOException {
        if (model != null) {
            for (String key : model.keySet()) {
                request.setAttribute(key, model.get(key));
            }
        }

        request.getRequestDispatcher("/WEB-INF/views/" + viewName + ".jsp").forward(request, response);
    }

    public static boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }


    public class HttpResponseException extends Exception {
        private int httpResponseCode;

        /**
         * @param httpResponseCode - see the <code>SC_</code> constants in {@link HttpServletResponse}
         * @param message
         */
        public HttpResponseException( int httpResponseCode, String message ) {
            super( message );
            this.httpResponseCode = httpResponseCode;
        }

        public HttpResponseException( int httpResponseCode ) {
            super();
            this.httpResponseCode = httpResponseCode;
        }

        public void sendError( HttpServletResponse response ) throws IOException {
            String message = this.getMessage();
            if( message != null ) {
                response.sendError( httpResponseCode, message );
            } else {
                response.sendError( httpResponseCode );
            }
        }
    }
}
