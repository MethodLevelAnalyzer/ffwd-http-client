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
import com.netflix.loadbalancer.Server;
import lombok.Data;
import okhttp3.OkHttpClient;

@Data
public class RawHttpClientFactory {
  private final ObjectMapper mapper;
  private final OkHttpClient httpClient;

  public RawHttpClient newClient(final Server server) {
    final String baseUrl = "http://" + server.getHost() + ":" + server.getPort();
    return new RawHttpClient(mapper, httpClient, baseUrl);
  }

  public void shutdown() {
    Exception e = null;

    try {
      httpClient.dispatcher().executorService().shutdown();
    } catch (final Exception inner) {
      e = inner;
    }

    try {
      httpClient.connectionPool().evictAll();
    } catch (final Exception inner) {
      if (e != null) {
        inner.addSuppressed(e);
      }

      e = inner;
    }

    if (httpClient.cache() != null) {
      try {
        httpClient.cache().close();
      } catch (final Exception inner) {
        if (e != null) {
          inner.addSuppressed(e);
        }

        e = inner;
      }
    }

    if (e != null) {
      throw new RuntimeException(e);
    }
  }
}
