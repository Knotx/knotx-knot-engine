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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.task.factory.TaskOptions.NODE_LOG_LEVEL_KEY;

import io.knotx.fragments.engine.Task;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.handler.action.ActionOptions;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.task.TaskDefinition;
import io.knotx.fragments.task.TaskFactory;
import io.knotx.fragments.task.exception.GraphConfigurationException;
import io.knotx.fragments.task.options.ActionNodeConfigOptions;
import io.knotx.fragments.task.options.GraphNodeOptions;
import io.knotx.fragments.task.options.SubtasksNodeConfigOptions;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultTaskFactory implements TaskFactory {

  public static final String NAME = "default";

  private ActionProvider actionProvider;

  public DefaultTaskFactory() {
    actionProvider = new ActionProvider(supplyFactories());
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Task newInstance(TaskDefinition taskDefinition, JsonObject taskOptions, Vertx vertx) {
    TaskOptions options = new TaskOptions(taskOptions);
    Map<String, ActionOptions> actionNameToOptions = options.getActions();
    if (actionNameToOptions == null) {
      throw new GraphConfigurationException("The 'actions' property not configured!");
    }
    initOptions(actionNameToOptions, options.getLogLevel());

    Node rootNode = initGraphRootNode(taskDefinition.getGraphNodeOptions(), actionNameToOptions,
        vertx);
    return new Task(taskDefinition.getTaskName(), rootNode);
  }

  private Node initGraphRootNode(GraphNodeOptions nodeOptions,
      Map<String, ActionOptions> actionNameToOptions, Vertx vertx) {
    Map<String, GraphNodeOptions> transitions = nodeOptions.getOnTransitions();
    Map<String, Node> edges = new HashMap<>();
    transitions.forEach((transition, childGraphOptions) -> edges
        .put(transition, initGraphRootNode(childGraphOptions, actionNameToOptions, vertx)));
    final Node node;
    if (nodeOptions.isComposite()) {
      node = buildCompositeNode(nodeOptions, edges, actionNameToOptions, vertx);
    } else {
      node = buildActionNode(nodeOptions, edges, actionNameToOptions, vertx);
    }
    return node;
  }

  private Node buildActionNode(GraphNodeOptions options, Map<String, Node> edges,
      Map<String, ActionOptions> actionNameToOptions, Vertx vertx) {
    ActionNodeConfigOptions config = new ActionNodeConfigOptions(options.getNode().getConfig());
    Action action = actionProvider.get(config.getAction(), actionNameToOptions, vertx).orElseThrow(
        () -> new GraphConfigurationException("No provider for action " + config.getAction()));
    return new SingleNode(config.getAction(), toRxFunction(action), edges);
  }

  private Node buildCompositeNode(GraphNodeOptions options, Map<String, Node> edges,
      Map<String, ActionOptions> actionNameToOptions, Vertx vertx) {
    SubtasksNodeConfigOptions config = new SubtasksNodeConfigOptions(
        options.getNode().getConfig());
    List<Node> nodes = config.getSubtasks().stream()
        .map((GraphNodeOptions o) -> initGraphRootNode(o, actionNameToOptions, vertx))
        .collect(Collectors.toList());
    return new CompositeNode(getNodeId(), nodes, edges.get(SUCCESS_TRANSITION),
        edges.get(ERROR_TRANSITION));
  }

  private String getNodeId() {
    // TODO this value should be calculated based on graph, the behaviour now is not changed
    return "composite";
  }

  private Function<FragmentContext, Single<FragmentResult>> toRxFunction(
      Action action) {
    io.knotx.fragments.handler.reactivex.api.Action rxAction = io.knotx.fragments.handler.reactivex.api.Action
        .newInstance(action);
    return rxAction::rxApply;
  }

  private void initOptions(Map<String, ActionOptions> nodeNameToOptions, String logLevel) {
    nodeNameToOptions.values().stream()
        .map(options -> {
          JsonObject config = options.getConfig();
          if (config.fieldNames().contains(NODE_LOG_LEVEL_KEY)) {
            return config;
          }
          return config.put(NODE_LOG_LEVEL_KEY, logLevel);
        });
  }

  private Supplier<Iterator<ActionFactory>> supplyFactories() {
    return () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader
          .load(ActionFactory.class);
      return factories.iterator();
    };
  }

}