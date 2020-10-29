/*-
 * -\-\-
 * FastForward HTTP Client
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.ffwd.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import okhttp3.OkHttpClient;
import org.mockserver.model.HttpRequest;


import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RawHttpClientTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private MockServerClient mockServerClient;

    private final ObjectMapper mapper = HttpClient.Builder.setupApplicationJson();

    private final OkHttpClient okHttpClient = new OkHttpClient();

    private RawHttpClient rawHttpClient;

    @Before
    public void setUp() throws Exception {
        mockServer.getPort();

        rawHttpClient =
            new RawHttpClient(mapper, okHttpClient, "http://localhost:" + mockServer.getPort());
    }

    @After
    public void tearDown() throws Exception {
        okHttpClient.connectionPool().evictAll();
        okHttpClient.dispatcher().executorService().shutdown();

        if (okHttpClient.cache() != null) {
            okHttpClient.cache().close();
        }
    }

    @Test
    public void testPing() {
        mockServerClient
            .when(request().withMethod("GET").withPath("/ping"))
            .respond(response().withStatusCode(200));
        rawHttpClient.ping().toCompletable().await();
    }

    @Test
    public void testSendBatchSuccess() {
        final String batchRequest =
            "{\"commonTags\":{\"what\":\"error-rate\"},\"commonResource\":{},\"points\":"
            + "[{\"key\":\"test_key\",\"tags\":{\"what\":\"error-rate\"},\"resource\":"
            + "{},\"value\":1234.0,\"timestamp\":11111}]}";

        mockServerClient
            .when(request()
                .withMethod("POST")
                .withPath("/v1/batch")
                .withHeader("content-type", "application/json")
                .withBody(batchRequest))
            .respond(response().withStatusCode(200));

        rawHttpClient.sendBatch(TestUtils.BATCH).toCompletable().await();
    }

    @Test
    public void testSendBatchSuccessV2() {
        final String batchRequest = TestUtils.createJsonString(TestUtils.BATCH_V2);
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/" + RawHttpClient.V2_BATCH_ENDPOINT)
                        .withHeader("content-type", "application/json")
                        .withBody(batchRequest))
                .respond(response().withStatusCode(200));

        rawHttpClient.sendBatch(TestUtils.BATCH_V2).toCompletable().await();
    }

    @Test
    public void testSendBatchFail() {
        expected.expectMessage("500: Internal Server Error");

        final String batchRequest =
            "{\"commonTags\":{\"what\":\"error-rate\"},\"commonResource\":{},\"points\":"
            + "[{\"key\":\"test_key\",\"tags\":{\"what\":\"error-rate\"},\"resource\":"
            + "{},\"value\":1234.0,\"timestamp\":11111}]}";

        mockServerClient
            .when(request()
                .withMethod("POST")
                .withPath("/v1/batch")
                .withHeader("content-type", "application/json")
                .withBody(batchRequest))
            .respond(response().withStatusCode(500));

        rawHttpClient.sendBatch(TestUtils.BATCH).toCompletable().await();
    }
}
