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
package io.knotx.fragments.task.handler.consumer;

import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.engine.FragmentEvent;
import io.knotx.fragments.task.factory.api.metadata.NodeMetadata;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskMetadata;
import io.knotx.fragments.task.handler.consumer.NodeExecutionData.Response;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeErrorLog;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeExecutionLog;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeOperationLog;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeResponseLog;
import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class MetadataConverter {

  private final String rootNodeId;
  private final Map<String, NodeMetadata> nodes;
  private final EventLogConverter eventLogConverter;

  MetadataConverter(FragmentEvent event, TaskMetadata taskMetadata) {
    this.rootNodeId = taskMetadata.getRootNodeId();
    this.nodes = taskMetadata.getNodesMetadata();
    this.eventLogConverter = new EventLogConverter(event.getLog().getOperations());
  }

  GraphNodeExecutionLog getExecutionLog() {
    return getExecutionLog(rootNodeId);
  }

  private GraphNodeExecutionLog getExecutionLog(String nodeId) {
    GraphNodeExecutionLog graphLog = fromMetadata(nodeId);
    NodeExecutionData nodeExecutionData = eventLogConverter.getExecutionData(nodeId);

    setGraphLogPropertiesFrom(graphLog, nodeExecutionData);

    if (containsUnsupportedTransitions(graphLog)) {
      addMissingNode(graphLog);
    }

    return graphLog;
  }

  private void setGraphLogPropertiesFrom(GraphNodeExecutionLog graphLog,
      NodeExecutionData executionData) {
    graphLog.setStatus(executionData.getStatus());
    graphLog.setStarted(executionData.getStarted());
    graphLog.setFinished(executionData.getFinished());

    Response metadataResponse = executionData.getResponse();
    if (metadataResponse != null) {
      graphLog
          .setResponse(GraphNodeResponseLog.newInstance(metadataResponse.getTransition(),
              metadataResponse.getLog(), getErrorLogs(metadataResponse)));
    }
  }

  private List<GraphNodeErrorLog> getErrorLogs(Response metadataResponse) {
    return metadataResponse.getErrors().stream()
        .map(GraphNodeErrorLog::newInstance)
        .collect(Collectors.toList());
  }

  private boolean containsUnsupportedTransitions(GraphNodeExecutionLog graphLog) {
    String transition = graphLog.getResponse().getTransition();
    return transition != null
        && !SUCCESS_TRANSITION.equals(transition)
        && !graphLog.getOn().containsKey(transition);
  }

  private void addMissingNode(GraphNodeExecutionLog graphLog) {
    GraphNodeExecutionLog missingNode = GraphNodeExecutionLog
        .newInstance(UUID.randomUUID().toString())
        .setType(NodeType.SINGLE)
        .setLabel("!")
        .setStarted(0)
        .setFinished(0)
        .setSubtasks(Collections.emptyList())
        .setOperation(null)
        .setOn(Collections.emptyMap())
        .setStatus(LoggedNodeStatus.MISSING);

    graphLog.getOn().put(graphLog.getResponse().getTransition(), missingNode);
  }

  private GraphNodeExecutionLog fromMetadata(String id) {
    if (nodes.containsKey(id)) {
      NodeMetadata metadata = nodes.get(id);

      return GraphNodeExecutionLog.newInstance(metadata.getNodeId())
          .setType(metadata.getType())
          .setLabel(metadata.getLabel())
          .setSubtasks(getSubTasks(metadata.getNestedNodes()))
          .setOperation(getOperationLog(metadata))
          .setOn(getTransitions(metadata.getTransitions()));
    } else {
      return GraphNodeExecutionLog.newInstance(id);
    }
  }

  private List<GraphNodeExecutionLog> getSubTasks(List<String> nestedNodes) {
    return nestedNodes.stream()
        .map(this::getExecutionLog)
        .collect(Collectors.toList());
  }

  private GraphNodeOperationLog getOperationLog(NodeMetadata metadata) {
    OperationMetadata operationMetadata = metadata.getOperation();
    return GraphNodeOperationLog
        .newInstance(operationMetadata.getFactory(), operationMetadata.getData());
  }

  private Map<String, GraphNodeExecutionLog> getTransitions(
      Map<String, String> definedTransitions) {
    Map<String, GraphNodeExecutionLog> result = new HashMap<>();
    definedTransitions.forEach((name, nextId) -> result.put(name, getExecutionLog(nextId)));
    return result;
  }
}
