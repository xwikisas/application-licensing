/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.internal.helpers;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provide various tools to handle HTTP methods usage.
 *
 * @version $Id$
 * @since 1.27
 */
@Component(roles = HttpClientUtils.class)
@Singleton
public class HttpClientUtils
{
    @Inject
    private Logger logger;

    /**
     * Execute a HttpPost and parse its response in JSON format.
     *
     * @param httpPost the HttpPost to execute
     * @param errorMsg the error message to log when an error occurs
     * @return response in JSON format
     */
    public JsonNode httpPost(HttpPost httpPost, String errorMsg)
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            JsonFactory jsonFactory = new JsonFactory();
            ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

            return client.execute(httpPost, response -> {
                final HttpEntity responseEntity = response.getEntity();
                if (responseEntity == null) {
                    return null;
                }
                try (InputStream inputStream = responseEntity.getContent()) {
                    return objectMapper.readTree(inputStream);
                }
            });
        } catch (IOException e) {
            logger.error("{} Root cause: [{}]", errorMsg, ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }
}
