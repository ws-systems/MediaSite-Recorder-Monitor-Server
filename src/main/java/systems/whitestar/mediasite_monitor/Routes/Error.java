package systems.whitestar.mediasite_monitor.Routes;

import lombok.extern.log4j.Log4j;
import org.jtwig.web.servlet.JtwigRenderer;
import systems.whitestar.mediasite_monitor.Models.SimpleMessage;
import systems.whitestar.mediasite_monitor.Secret;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;


/**
 * Error Page Route (For when things don't go as intended).
 *
 * @author Tom Paulus
 * Created on 10/28/17.
 */
@Log4j
public class Error extends HttpServlet {
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/error.twig";
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = (String) request.getAttribute("javax.servlet.forward.request_uri");
        int code = response.getStatus();
        String message = (String) request.getAttribute("javax.servlet.error.message");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");

        if (exception != null) {
            log.warn(String.format("%s - %d %s", path, code, exception.getMessage()), exception.getCause());
        }

        if (path == null || !path.startsWith("/api/")) {
            log.debug(String.format("Request to %s returned Error Code %d", path, response.getStatus()));
            // Ignore all requests to API Endpoints, since they handel problems in a different way

            request.setAttribute("hide_issue_link", Secret.getInstance().getSecret("issues.hideLink"));
            request.setAttribute("issue_link", Secret.getInstance().getSecret("issues.link"));

            request.setAttribute("show_stacktrace", Secret.getInstance().getSecret("debug.showStacktrace"));

            request.setAttribute("code", code);

            if (exception != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(exception.getMessage());
                sb.append("\n");

                for (StackTraceElement element : exception.getCause().getStackTrace()) {
                    sb.append(element.toString());
                    sb.append("\n");
                }

                request.setAttribute("stacktrace", sb.toString());
            }

            switch (code) {
                case 200:
                    request.setAttribute("title", "What?");
                    request.setAttribute("message", "So, you found the \"error\" page...");
                    String[] videos = new String[]{
                            "https://www.youtube.com/embed/NfPndEB2ec0",
                            "https://www.youtube.com/embed/w0CdXaOS5_o",
                            "https://www.youtube.com/embed/dQw4w9WgXcQ"
                    };

                    request.setAttribute("stacktrace",
                            String.format("<iframe width=\"560\" height=\"315\" src=\"%s?rel=0&showinfo=0\"" +
                                    "frameborder=\"0\" allowfullscreen></iframe>", videos[new Random().nextInt(videos.length)]));
                    break;
                case 401:
                case 403:
                    request.setAttribute("title", "Sorry, you can't be\n back here.");
                    request.setAttribute("message", "You are not allowed to access this page,\n" +
                            "if it even exists. Please try logging in.");
                    break;
                case 405:
                    request.setAttribute("title", "No. Just No.");
                    request.setAttribute("message", "You are not allowed to do that here.\n" +
                            "Now please stop.");
                    break;
                case 404:
                    request.setAttribute("title", "Oops, the page you're\n looking for does not exist.");
                    request.setAttribute("message", "You may want to head back to the homepage.\n" +
                            "If you think something is broken, report a problem.");
                    break;
                case 500:
                    request.setAttribute("title", "Something broke unexpectedly,\nand we do apologize for that.");
                    request.setAttribute("message", "This sort of thing doesn't usually happen.\n" +
                            "If this keeps happening, please let us know.");
                    break;
                default:
                    request.setAttribute("title", "Well, this is embarrassing.");
                    request.setAttribute("message", "You usually aren't supposed to see this page, \nso " +
                            "something must have gone wrong. We are sorry! Trying again may fix it.");
                    break;
            }

            response.addHeader("Content-Type", MediaType.TEXT_HTML);
            renderer.dispatcherFor(TEMPLATE_PATH)
                    .render(request, response);
        } else {
            // API Error
            response.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            final PrintWriter writer = response.getWriter();

            switch (response.getStatus()) {
                case 400:
                case 401:
                case 402:
                case 403:
                case 404:
                case 405:
                case 406:
                case 407:
                case 408:
                case 409:
                case 410:
                case 411:
                case 412:
                case 413:
                case 414:
                case 415:
                case 416:
                case 417:
                    writer.write(new SimpleMessage("Error - " + response.getStatus(),
                            "There was a problem with your request. Trying again " +
                                    "will probably not fix the problem.").asJson());
                    break;

                case 500:
                case 501:
                case 502:
                case 503:
                case 504:
                case 505:
                    writer.write(new SimpleMessage("Error - " + response.getStatus(),
                            "We had some problems with your request. Trying again " +
                                    "may fix this issue. It may not.").asJson());
                    break;

                default:
                    writer.write(new SimpleMessage("Error",
                            "We had some problems with your request. That's all " +
                                    "we know.").asJson());
                    break;
            }

            writer.close();
        }
    }
}
