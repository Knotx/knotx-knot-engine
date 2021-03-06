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
package io.knotx.fragments.task.engine;

import static io.knotx.fragments.task.engine.FragmentEventLogVerifier.verifyAllLogEntries;
import static io.knotx.fragments.task.engine.Nodes.composite;
import static io.knotx.fragments.task.engine.Nodes.single;
import static io.knotx.fragments.task.engine.TestFunction.appendBody;
import static io.knotx.fragments.task.engine.TestFunction.appendBodyWithPayload;
import static io.knotx.fragments.task.engine.TestFunction.appendPayload;
import static io.knotx.fragments.task.engine.TestFunction.appendPayloadBasingOnContext;
import static io.knotx.fragments.task.engine.TestFunction.failure;
import static io.knotx.fragments.task.engine.TestFunction.success;
import static io.knotx.fragments.task.engine.TestFunction.successWithDelay;
import static io.knotx.fragments.task.engine.Transitions.onError;
import static io.knotx.fragments.task.engine.Transitions.onSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.engine.FragmentEvent.Status;
import io.knotx.fragments.task.engine.FragmentEventLogVerifier.Operation;
import io.knotx.fragments.task.api.Node;
import io.knotx.junit5.util.RequestUtil;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskEngineScenariosTest {

  private static final String COMPOSITE_NODE_ID = "composite";
  private static final String INITIAL_BODY = "initial body";

  private FragmentEventContext eventContext;

  @BeforeEach
  void setUp() {
    Fragment initialFragment = new Fragment("snippet", new JsonObject(), INITIAL_BODY);
    eventContext = new FragmentEventContext(new FragmentEvent(initialFragment),
        new ClientRequest());
  }

  /*
   * scenario: first -> parallel[A,B,C] -> last
   */
  @Test
  @DisplayName("Expect success status and fragment's body update when parallel processing")
  void expectSuccessParallelProcessing(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    JsonObject taskAPayload = new JsonObject().put("key", "taskAOperation");
    JsonObject taskBPayload = new JsonObject().put("key", "taskBOperation");
    JsonObject taskCPayload = new JsonObject().put("key", "taskCOperation");

    Node rootNode = single("first", appendBody(":first"), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", appendPayload("A", taskAPayload)),
                single("B", appendPayload("B", taskBPayload)),
                single("C", appendPayload("C", taskCPayload))),
            single("last", appendBody(":last")))));
    String expectedBody = INITIAL_BODY + ":first:last";

    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          final Fragment fragment = fragmentEvent.getFragment();
          assertEquals(expectedBody, fragment.getBody());
          final JsonObject payload = fragment.getPayload();
          assertEquals(taskAPayload, payload.getJsonObject("A"));
          assertEquals(taskBPayload, payload.getJsonObject("B"));
          assertEquals(taskCPayload, payload.getJsonObject("C"));
        });
  }

  /*
   * scenario: first -> parallel[A, B] -> middle -> parallel[X, Y] -> last
   * X uses payload from A,
   * Y uses payload from B,
   * last uses payload from X and Y to append body
   */
  @Test
  @DisplayName("Expect body updated after complex processing")
  void expectSuccessMultipleParallel(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", appendPayload("A", ":payloadA")),
                single("B", appendPayload("B", ":payloadB"))),
            single("middle", success(), onSuccess(
                composite(COMPOSITE_NODE_ID,
                    parallel(
                        single("X", appendPayloadBasingOnContext("A", "X", "withX")),
                        single("Y", appendPayloadBasingOnContext("B", "Y", "withY"))),
                    single("last", appendBodyWithPayload("X", "Y"))))))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);
    String expectedBody = INITIAL_BODY + ":payloadAwithX:payloadBwithY";

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          Assertions.assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          final Fragment fragment = fragmentEvent.getFragment();
          assertEquals(expectedBody, fragment.getBody());
        });
  }

  /*
   * scenario: scenario: first -> parallelA-B[A1 -> A2 -error-> A3(fallback), B] -> middle -> parallelX-Y[X, Y1 -> Y2] -> last
   */
  @Test
  @DisplayName("Expect logs in order after complex processing")
  void expectProcessingLogsInOrder(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Exception nodeException = new IllegalArgumentException("Some node error message");
    Node rootNode =
        single("first", success(), onSuccess(
            composite("parallelA-B",
                parallel(
                    single("A1", success(), onSuccess(
                        single("A2", failure(nodeException), onError(
                            single("A3-fallback", success()))))),
                    single("B", success())),
                // onSuccess
                single("middle", success(), onSuccess(
                    composite("parallelX-Y",
                        parallel(
                            single("X", success()),
                            single("Y1", success(), onSuccess(
                                single("Y2", success())))),
                        single("last", success())))))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          verifyAllLogEntries(
              fragmentEvent.getLog().getOperations(), Operation.exact("task", "first", "UNPROCESSED", 0),
              Operation.exact("task", "first", "SUCCESS", 1),
              Operation.exact("task", "parallelA-B", "UNPROCESSED", 2),
              Operation.range("task", "A1", "UNPROCESSED", 3, 9),
              Operation.range("task", "A2", "UNPROCESSED", 3, 9),
              Operation.range("task", "A3-fallback", "UNPROCESSED", 3, 9),
              Operation.range("task", "B", "UNPROCESSED", 3, 9),
              Operation.range("task", "A1", "SUCCESS", 4, 10),
              Operation.range("task", "A2", "ERROR", 4, 10, nodeException),
              Operation.range("task", "A3-fallback", "SUCCESS", 4, 10),
              Operation.range("task", "B", "SUCCESS", 4, 10),
              Operation.exact("task", "parallelA-B", "SUCCESS", 11),
              Operation.exact("task", "middle", "UNPROCESSED", 12),
              Operation.exact("task", "middle", "SUCCESS", 13),
              Operation.exact("task", "parallelX-Y", "UNPROCESSED", 14),
              Operation.range("task", "X", "UNPROCESSED", 15, 19),
              Operation.range("task", "Y1", "UNPROCESSED", 15, 19),
              Operation.range("task", "Y2", "UNPROCESSED", 15, 19),
              Operation.range("task", "X", "SUCCESS", 16, 20),
              Operation.range("task", "Y1", "SUCCESS", 16, 20),
              Operation.range("task", "Y2", "SUCCESS", 16, 20),
              Operation.exact("task", "parallelX-Y", "SUCCESS", 21),
              Operation.exact("task", "last", "UNPROCESSED", 22),
              Operation.exact("task", "last", "SUCCESS", 23)
          );
        });
  }

  /*
   * scenario: first -> parallel[A, B modifies body: FATAL, C] -> last
   * FATAL after parallel
   */
  @Test
  @Disabled
  @DisplayName("Expect fatal status when body is modified during parallel processing")
  void ensureBodyImmutableDuringParallelProcessing(VertxTestContext testContext, Vertx vertx) {
    // ToDo: TBD if we want to implement it
  }

  /*
   * scenario: first -> parallel[A, B, C] -> last
   *  A, B, C all with 500 ms delay, 1s for parallel section
   */
  @Test
  @DisplayName("Expect parallel nodes processed in parallel when delays")
  void verifyParallelExecution(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", successWithDelay(500, vertx)),
                single("B", successWithDelay(500, vertx)),
                single("C", successWithDelay(500, vertx))),
            single("last", success()))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> assertEquals(Status.SUCCESS, fragmentEvent.getStatus()));
  }

  /*
   * scenario: parallel[A, parallel[B -> B1, C], D] -> last
   *  A, B, C, D all with 500 ms delay, 1s for parallel section
   */
  @Test
  @DisplayName("Expect nested parallel nodes processed in parallel when delays")
  void verifyNestedParallelExecution(VertxTestContext testContext, Vertx vertx) throws Throwable {
    // given
    Node rootNode = single("first", success(), onSuccess(
        composite(COMPOSITE_NODE_ID,
            parallel(
                single("A", successWithDelay(500, vertx)),
                composite(COMPOSITE_NODE_ID,
                    parallel(
                        single("B", successWithDelay(500, vertx), onSuccess(
                            single("B1", appendPayload("B1", "B1Payload")))),
                        single("C", successWithDelay(500, vertx)))),
                single("D", successWithDelay(500, vertx))),
            single("last", success()))));
    // when
    Single<FragmentEvent> result = new TaskEngine(vertx).start("task", rootNode, eventContext);

    // then
    verifyExecution(result, testContext,
        fragmentEvent -> {
          assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
          JsonObject payload = fragmentEvent.getFragment().getPayload();
          assertEquals("B1Payload", payload.getString("B1"));
        });
  }

  private List<Node> parallel(Node... nodes) {
    return Arrays.asList(nodes);
  }

  private void verifyExecution(Single<FragmentEvent> result, VertxTestContext testContext,
      Consumer<FragmentEvent> successConsumer) throws Throwable {
    RequestUtil.subscribeToResult_shouldSucceed(testContext, result, successConsumer);
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}