/* (C)2023 */
package it.pagopa.pn.delivery.utils.io;

import it.pagopa.tech.lollipop.consumer.command.LollipopConsumerCommand;
import it.pagopa.tech.lollipop.consumer.command.LollipopConsumerCommandBuilder;
import it.pagopa.tech.lollipop.consumer.model.CommandResult;
import it.pagopa.tech.lollipop.consumer.utils.LollipopConsumerConverter;
import lombok.AllArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.pagopa.tech.lollipop.consumer.command.impl.LollipopConsumerCommandImpl.VERIFICATION_SUCCESS_CODE;

/**
 * Instance of a Spring Http {@link HandlerInterceptor}, to be used for Lollipop Request validations
 */
@AllArgsConstructor
public class HttpVerifierHandlerInterceptor implements HandlerInterceptor {

    private final LollipopConsumerCommandBuilder consumerCommandBuilder;
    private static final Log log = LogFactory.getLog(HttpVerifierHandlerInterceptor.class);

    /**
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return boolean to determine if the handle completes successfully
     * @throws IOException throws exception if the conversion of a http request fails
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String contentType = request.getContentType();
        if (contentType != null && contentType.equals("application/io+json")) {
            LollipopConsumerCommand lollipopConsumerCommand =
                    consumerCommandBuilder.createCommand(
                            LollipopConsumerConverter.convertToLollipopRequest(request));

            try {
                CommandResult commandResult = lollipopConsumerCommand.doExecute();

                LollipopConsumerConverter.interceptResult(commandResult, response);

                if (commandResult.getResultCode().equals(VERIFICATION_SUCCESS_CODE)) {
                    return true;
                }

            } catch (Exception e) {
                log.error("Error verifying request", e);
            }

            return false;
        } else {
            return true;
        }
    }
}
