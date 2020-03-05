package io.knotx.fragments.handler;

import static io.knotx.fragments.engine.EventLogEntry.NodeStatus;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.engine.api.node.single.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.engine.EventLogEntry;

import java.util.Arrays;

public enum LoggedNodeStatus {
  SUCCESS {
    @Override
    protected boolean isEquivalent(NodeStatus status, String transition) {
      return status == NodeStatus.SUCCESS && SUCCESS_TRANSITION.equals(transition);
    }
  },

  ERROR {
    @Override
    protected boolean isEquivalent(NodeStatus status, String transition) {
      return status == NodeStatus.ERROR && ERROR_TRANSITION.equals(transition);
    }
  },

  OTHER {
    @Override
    protected boolean isEquivalent(NodeStatus status, String transition) {
      return !SUCCESS_TRANSITION.equals(transition) && !ERROR_TRANSITION.equals(transition);
    }
  },

  MISSING {
    @Override
    protected boolean isEquivalent(NodeStatus status, String transition) {
      return status == NodeStatus.UNSUPPORTED_TRANSITION;
    }
  },

  UNPROCESSED {
    @Override
    protected boolean isEquivalent(NodeStatus status, String transition) {
      return status == NodeStatus.UNPROCESSED;
    }
  };

  public static LoggedNodeStatus from(EventLogEntry logEntry) {
    return Arrays.stream(LoggedNodeStatus.values())
        .filter(status -> status.isEquivalent(logEntry.getStatus(), logEntry.getTransition()))
        .findAny()
        .orElseThrow(IllegalArgumentException::new);
  }

  protected abstract boolean isEquivalent(NodeStatus status, String transition);
}
