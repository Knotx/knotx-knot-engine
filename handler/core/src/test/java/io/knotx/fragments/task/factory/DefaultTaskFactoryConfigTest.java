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
package io.knotx.fragments.task.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.HoconLoader;
import io.knotx.fragments.task.factory.node.action.ActionNodeConfig;
import io.knotx.fragments.task.factory.node.action.ActionNodeFactory;
import io.knotx.fragments.task.factory.node.subtasks.SubtasksNodeConfig;
import io.knotx.fragments.task.factory.node.subtasks.SubtasksNodeFactory;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class DefaultTaskFactoryConfigTest {

  @Test
  @DisplayName("Expect task with action node when simplified task definition.")
  void expectActionNodeWhenSimplifiedTask(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskSimplified.conf", validateActionNode(), vertx);
  }

  @Test
  @DisplayName("Expect task with action node when action defined directly in the task.")
  void expectActionNodeWhenActionDirectlyDefinedInGraph(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithActionNode.conf", validateActionNode(), vertx);
  }

  @Test
  @DisplayName("Expect task with Action node when action is configured.")
  void expectActionNodeWhenActionDefined(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithActionNode-fullSyntax.conf", validateActionNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when actions is configured")
  void expectSubTaskNodeWhenActionsDefined(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithSubtasksDeprecated.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when subtasks directly is configured")
  void expectSubtasksNodeWhenSubtasksDirectlyDefined(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithSubtasks.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect subtasks node when subtasks is configured")
  void expectSubtasksNodeWhenSubtasksDefined(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithSubtasks-fullSyntax.conf", validateSubtasksNode(), vertx);
  }

  @Test
  @DisplayName("Expect nodes configured with _success flow")
  void expectTransitionSuccessWithNodeBThenNodeC(Vertx vertx) throws Throwable {
    HoconLoader.verify("task/taskWithTransitions.conf", config -> {
      GraphNodeOptions graphNodeOptions = new TaskOptions(config).getGraph();
      Optional<GraphNodeOptions> nodeB = graphNodeOptions.get("_success");
      assertTrue(nodeB.isPresent());
      assertEquals("b", getAction(nodeB.get()));
      Optional<GraphNodeOptions> nodeC = nodeB.get().get("_success");
      assertTrue(nodeC.isPresent());
      assertEquals("c", getAction(nodeC.get()));
    }, vertx);
  }


  private Consumer<JsonObject> validateActionNode() {
    return config -> {
      GraphNodeOptions graphNodeOptions = new TaskOptions(config).getGraph();
      assertEquals("a", getAction(graphNodeOptions));
      assertEquals(ActionNodeFactory.NAME, graphNodeOptions.getNode().getFactory());
    };
  }

  private Consumer<JsonObject> validateSubtasksNode() {
    return config -> {
      TaskOptions taskOptions = new TaskOptions(config);
      GraphNodeOptions graphNodeOptions = taskOptions.getGraph();
      assertEquals(SubtasksNodeFactory.NAME, graphNodeOptions.getNode().getFactory());
      SubtasksNodeConfig nodeConfig = new SubtasksNodeConfig(
          graphNodeOptions.getNode().getConfig());
      assertEquals(2, nodeConfig.getSubtasks().size());
    };
  }

  private String getAction(GraphNodeOptions graphNodeOptions) {
    return new ActionNodeConfig(
        graphNodeOptions.getNode().getConfig()).getAction();
  }
}