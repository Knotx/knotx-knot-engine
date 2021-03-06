/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.fragments.action.library.http.options;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Describes a physical details of HTTP service endpoint that Action will connect to.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class EndpointOptions {

  private String path;
  private String body = StringUtils.EMPTY;
  private JsonObject bodyJson = new JsonObject();
  private String domain;
  private int port;
  private Set<String> allowedRequestHeaders;
  private JsonObject additionalHeaders;
  private List<Pattern> allowedRequestHeadersPatterns;
  private boolean interpolatePath = true;
  private boolean interpolateBody = false;
  private boolean clearUnmatchedPlaceholdersInPath = true;
  private boolean clearUnmatchedPlaceholdersInBodyString = false;
  private boolean clearUnmatchedPlaceholdersInBodyJson = false;
  private boolean encodePlaceholdersInPath = true;
  private boolean encodePlaceholdersInBodyString = false;
  private boolean encodePlaceholdersInBodyJson = false;
  //ToDo: private Set<StatusCode> successStatusCodes;

  public EndpointOptions() {
    //empty default constructor
  }

  public EndpointOptions(EndpointOptions other) {
    this.path = other.path;
    this.body = other.body;
    this.domain = other.domain;
    this.port = other.port;
    this.allowedRequestHeaders = new HashSet<>(other.allowedRequestHeaders);
    this.allowedRequestHeadersPatterns = new ArrayList<>(other.allowedRequestHeadersPatterns);
    this.additionalHeaders = other.additionalHeaders.copy();
    this.bodyJson = other.getBodyJson();
    this.interpolatePath = other.isInterpolatePath();
    this.interpolateBody = other.isInterpolateBody();
    this.clearUnmatchedPlaceholdersInPath = other.clearUnmatchedPlaceholdersInPath;
    this.clearUnmatchedPlaceholdersInBodyString = other.clearUnmatchedPlaceholdersInBodyString;
    this.clearUnmatchedPlaceholdersInBodyJson = other.clearUnmatchedPlaceholdersInBodyJson;
    this.encodePlaceholdersInPath = other.encodePlaceholdersInPath;
    this.encodePlaceholdersInBodyString = other.encodePlaceholdersInBodyString;
    this.encodePlaceholdersInBodyJson = other.encodePlaceholdersInBodyJson;
  }

  public EndpointOptions(JsonObject json) {
    this();
    EndpointOptionsConverter.fromJson(json, this);
    if (allowedRequestHeaders != null) {
      allowedRequestHeadersPatterns = allowedRequestHeaders.stream()
          .map(Pattern::compile).collect(Collectors.toList());
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    EndpointOptionsConverter.toJson(this, json);
    return json;
  }

  public String getPath() {
    return path;
  }

  /**
   * Sets the request path to the endpoint. The request path may contain <a
   * href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x Server
   * Common Placeholders</a> referencing ClientRequest, Fragment's configuration or Fragment's
   * payload.
   *
   * @param path an endpoint request path.
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setPath(String path) {
    this.path = path;
    return this;
  }

  public String getBody() {
    return body;
  }

  /**
   * Sets the request body schema to be sent to the endpoint. The body may contain <a
   * href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x Server
   * Common Placeholders</a> referencing ClientRequest, Fragment's configuration or Fragment's
   * payload, which will be interpolated if {@link EndpointOptions#interpolateBody} flag is set.
   * <p>
   * Please note that request body is sent only in case of using PUT, POST or PATCH HTTP method.
   * <p>
   * This field is mutually exclusive with {@link EndpointOptions#bodyJson}.
   *
   * @param body a body to be send to the endpoint
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setBody(String body) {
    this.body = body;
    return this;
  }

  public JsonObject getBodyJson() {
    return bodyJson;
  }

  /**
   * Sets the request body schema to be sent to the endpoint. The body may contain <a
   * href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x Server
   * Common Placeholders</a> referencing ClientRequest, Fragment's configuration or Fragment's
   * payload, which will be interpolated if {@link EndpointOptions#interpolateBody} flag is set.
   * <p>
   * Please note that request body is sent only in case of using PUT, POST or PATCH HTTP method.
   * <p>
   * This field is mutually exclusive with {@link EndpointOptions#body}.
   *
   * @param bodyJson a body to be send to the endpoint
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setBodyJson(JsonObject bodyJson) {
    this.bodyJson = bodyJson;
    return this;
  }

  /**
   * @return a domain of the external service
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the {@code domain} of the external service
   *
   * @param domain - domain of the external service
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * @return HTTP port of the external service
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the HTTP {@code port} the external service
   *
   * @param port - HTTP port
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return Set of allowed request headers that should be passed-through to the service
   */
  public Set<String> getAllowedRequestHeaders() {
    return allowedRequestHeaders;
  }

  /**
   * Sets the allowed requests headers that should be send to the service. The selected headers from
   * the original client HTTP request are being send.
   *
   * @param allowedRequestHeaders set of Strings with header names
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setAllowedRequestHeaders(Set<String> allowedRequestHeaders) {
    this.allowedRequestHeaders = allowedRequestHeaders;
    allowedRequestHeadersPatterns = allowedRequestHeaders.stream()
        .map(Pattern::compile).collect(Collectors.toList());
    return this;
  }

  /**
   * @return a Json Object with additional headers and it's values
   */
  public JsonObject getAdditionalHeaders() {
    return additionalHeaders;
  }

  /**
   * Sets the additional request headers (and values) to be send in each request
   *
   * @param additionalHeaders - JSON Object specifying additional header
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setAdditionalHeaders(JsonObject additionalHeaders) {
    this.additionalHeaders = additionalHeaders;
    return this;
  }

  @GenIgnore
  public List<Pattern> getAllowedRequestHeadersPatterns() {
    return allowedRequestHeadersPatterns;
  }

  @GenIgnore
  public EndpointOptions setAllowedRequestHeaderPatterns(
      List<Pattern> allowedRequestHeaderPatterns) {
    this.allowedRequestHeadersPatterns = allowedRequestHeaderPatterns;
    return this;
  }

  public boolean isInterpolatePath() {
    return interpolatePath;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#path} parameter. When set, the path will be
   * interpolated using <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a> referencing ClientRequest, Fragment's configuration or
   * Fragment's payload.
   *
   * @param interpolatePath flag enabling path interpolation
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setInterpolatePath(boolean interpolatePath) {
    this.interpolatePath = interpolatePath;
    return this;
  }

  public boolean isInterpolateBody() {
    return interpolateBody;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#body} parameter. When set, the body will be
   * interpolated using <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a> referencing ClientRequest, Fragment's configuration or
   * Fragment's payload.
   *
   * @param interpolateBody flag enabling body interpolation
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setInterpolateBody(boolean interpolateBody) {
    this.interpolateBody = interpolateBody;
    return this;
  }

  public boolean isClearUnmatchedPlaceholdersInPath() {
    return clearUnmatchedPlaceholdersInPath;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#path} parameter. When path interpolation is
   * enabled and this flag is set, placeholders not matched to a source will be replaced with an
   * empty string.
   * <p>
   * Please note that absent placeholders that are matched to a source (e.g. {payload.not-existent})
   * will always be replaced with an empty string.
   *
   * @param clearUnmatchedPlaceholdersInPath flag enabling clearing unmatched placeholders in path
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setClearUnmatchedPlaceholdersInPath(
      boolean clearUnmatchedPlaceholdersInPath) {
    this.clearUnmatchedPlaceholdersInPath = clearUnmatchedPlaceholdersInPath;
    return this;
  }

  public boolean isClearUnmatchedPlaceholdersInBodyString() {
    return clearUnmatchedPlaceholdersInBodyString;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#body} parameter. When body interpolation is
   * enabled and this flag is set, placeholders not matched to a source will be replaced with an
   * empty string.
   * <p>
   * Please note that absent placeholders that are matched to a source (e.g. {payload.not-existent})
   * will always be replaced with an empty string.
   *
   * @param clearUnmatchedPlaceholdersInBodyString flag enabling clearing unmatched placeholders in
   *                                               body
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setClearUnmatchedPlaceholdersInBodyString(
      boolean clearUnmatchedPlaceholdersInBodyString) {
    this.clearUnmatchedPlaceholdersInBodyString = clearUnmatchedPlaceholdersInBodyString;
    return this;
  }

  public boolean isClearUnmatchedPlaceholdersInBodyJson() {
    return clearUnmatchedPlaceholdersInBodyJson;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#bodyJson} parameter. When JSON body
   * interpolation is enabled and this flag is set, placeholders not matched to a source will be
   * replaced with an empty string.
   * <p>
   * Please note that absent placeholders that are matched to a source (e.g. {payload.not-existent})
   * will always be replaced with an empty string.
   *
   * @param clearUnmatchedPlaceholdersInBodyJson flag enabling clearing unmatched placeholders in
   *                                             JSON body
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setClearUnmatchedPlaceholdersInBodyJson(
      boolean clearUnmatchedPlaceholdersInBodyJson) {
    this.clearUnmatchedPlaceholdersInBodyJson = clearUnmatchedPlaceholdersInBodyJson;
    return this;
  }

  public boolean isEncodePlaceholdersInPath() {
    return encodePlaceholdersInPath;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#path} parameter. When path interpolation is
   * enabled and this flag is set, values of matched placeholders will be encoded before
   * interpolating.
   * <p>
   * For details, see <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a>
   *
   * @param encodePlaceholdersInPath flag enabling placeholder encoding in path
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setEncodePlaceholdersInPath(boolean encodePlaceholdersInPath) {
    this.encodePlaceholdersInPath = encodePlaceholdersInPath;
    return this;
  }

  public boolean isEncodePlaceholdersInBodyString() {
    return encodePlaceholdersInBodyString;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#body} parameter. When body interpolation is
   * enabled and this flag is set, values of matched placeholders will be encoded before
   * interpolating.
   * <p>
   * For details, see <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a>
   *
   * @param encodePlaceholdersInBodyString flag enabling placeholder encoding in body
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setEncodePlaceholdersInBodyString(boolean encodePlaceholdersInBodyString) {
    this.encodePlaceholdersInBodyString = encodePlaceholdersInBodyString;
    return this;
  }

  public boolean isEncodePlaceholdersInBodyJson() {
    return encodePlaceholdersInBodyJson;
  }

  /**
   * Configures interpolation of {@link EndpointOptions#bodyJson} parameter. When JSON body
   * interpolation is enabled and this flag is set, values of matched placeholders will be encoded
   * before interpolating.
   * <p>
   * For details, see <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a>
   *
   * @param encodePlaceholdersInBodyJson flag enabling placeholder encoding in JSON body
   * @return a reference to this, so the API can be used fluently
   */
  public EndpointOptions setEncodePlaceholdersInBodyJson(boolean encodePlaceholdersInBodyJson) {
    this.encodePlaceholdersInBodyJson = encodePlaceholdersInBodyJson;
    return this;
  }

  @Override
  public String toString() {
    return "EndpointOptions{" +
        "path='" + path + '\'' +
        ", body='" + body + '\'' +
        ", bodyJson=" + bodyJson +
        ", domain='" + domain + '\'' +
        ", port=" + port +
        ", allowedRequestHeaders=" + allowedRequestHeaders +
        ", additionalHeaders=" + additionalHeaders +
        ", allowedRequestHeadersPatterns=" + allowedRequestHeadersPatterns +
        ", interpolatePath=" + interpolatePath +
        ", interpolateBody=" + interpolateBody +
        ", clearUnmatchedPlaceholdersInPath=" + clearUnmatchedPlaceholdersInPath +
        ", clearUnmatchedPlaceholdersInBodyString=" + clearUnmatchedPlaceholdersInBodyString +
        ", clearUnmatchedPlaceholdersInBodyJson=" + clearUnmatchedPlaceholdersInBodyJson +
        ", encodePlaceholdersInPath=" + encodePlaceholdersInPath +
        ", encodePlaceholdersInBodyString=" + encodePlaceholdersInBodyString +
        ", encodePlaceholdersInBodyJson=" + encodePlaceholdersInBodyJson +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EndpointOptions that = (EndpointOptions) o;
    return port == that.port && interpolatePath == that.interpolatePath
        && interpolateBody == that.interpolateBody
        && clearUnmatchedPlaceholdersInPath == that.clearUnmatchedPlaceholdersInPath
        && clearUnmatchedPlaceholdersInBodyString == that.clearUnmatchedPlaceholdersInBodyString
        && clearUnmatchedPlaceholdersInBodyJson == that.clearUnmatchedPlaceholdersInBodyJson
        && encodePlaceholdersInPath == that.encodePlaceholdersInPath
        && encodePlaceholdersInBodyString == that.encodePlaceholdersInBodyString
        && encodePlaceholdersInBodyJson == that.encodePlaceholdersInBodyJson && Objects
        .equals(path, that.path) && Objects.equals(body, that.body) && Objects
        .equals(bodyJson, that.bodyJson) && Objects.equals(domain, that.domain)
        && Objects.equals(allowedRequestHeaders, that.allowedRequestHeaders)
        && Objects.equals(additionalHeaders, that.additionalHeaders) && Objects
        .equals(allowedRequestHeadersPatterns, that.allowedRequestHeadersPatterns);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(path, body, bodyJson, domain, port, allowedRequestHeaders, additionalHeaders,
            allowedRequestHeadersPatterns, interpolatePath, interpolateBody,
            clearUnmatchedPlaceholdersInPath, clearUnmatchedPlaceholdersInBodyString,
            clearUnmatchedPlaceholdersInBodyJson, encodePlaceholdersInPath,
            encodePlaceholdersInBodyString, encodePlaceholdersInBodyJson);
  }
}
