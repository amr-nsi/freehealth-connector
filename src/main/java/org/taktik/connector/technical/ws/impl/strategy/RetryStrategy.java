package org.taktik.connector.technical.ws.impl.strategy;

import org.taktik.connector.technical.exception.NoNextEndpointException;
import org.taktik.connector.technical.exception.RetryNextEndpointException;
import org.taktik.connector.technical.exception.TechnicalConnectorException;
import org.taktik.connector.technical.exception.TechnicalConnectorExceptionValues;
import org.taktik.connector.technical.ws.domain.GenericRequest;
import org.taktik.connector.technical.ws.domain.GenericResponse;
import org.taktik.connector.technical.ws.impl.AbstractWsSender;
import be.fgov.ehealth.technicalconnector.bootstrap.bcp.EndpointDistributor;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryStrategy extends AbstractWsSender implements InvokeStrategy {
   private static final Logger LOG = LoggerFactory.getLogger(RetryStrategy.class);
   private static EndpointDistributor distributor = EndpointDistributor.getInstance();

   public GenericResponse invoke(GenericRequest genericRequest) throws TechnicalConnectorException {
      RetryStrategy.RetryContext ctx = new RetryStrategy.RetryContext(this.getCurrentEndpoint(genericRequest));
      int alternatives = distributor.getAmountOfAlternatives(ctx.endpoint);

      for(int i = 0; i < alternatives; ++i) {
         String activeEndpoint = distributor.getActiveEndpoint(ctx.endpoint);
         if (!ctx.invokedEndpoints.contains(activeEndpoint)) {
            ctx.invokedEndpoints.add(activeEndpoint);
            genericRequest.setEndpoint(activeEndpoint);

            try {
               GenericResponse resp = super.call(genericRequest);
               if (ctx.alternativeActivated) {
                  LOG.debug("Activating status page polling!");
                  distributor.activatePolling();
               }

               return resp;
            } catch (RetryNextEndpointException var9) {
               LOG.error("Unable to invoke endpoint [{}], activating next one.", activeEndpoint, var9);

               try {
                  distributor.activateNextEndPoint(activeEndpoint);
                  ctx.alternativeActivated = true;
               } catch (NoNextEndpointException var8) {
                  LOG.error("Unable to activate alternative", var8);
               }

               ctx.lastException = var9;
            }
         } else {
            LOG.debug("Endpoint [{}] already invoked, skipping it.", activeEndpoint);
         }
      }

      if (EndpointDistributor.update()) {
         return this.invoke(genericRequest);
      } else {
         throw new TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_WS, ExceptionUtils.getRootCause(ctx.lastException), new Object[]{ExceptionUtils.getRootCauseMessage(ctx.lastException)});
      }
   }

   private static class RetryContext {
      protected String endpoint;
      protected Exception lastException;
      protected boolean alternativeActivated;
      protected List<String> invokedEndpoints = new ArrayList();

      public RetryContext(String endpoint) {
         this.endpoint = endpoint;
      }
   }
}
