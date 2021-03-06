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
package io.knotx.fragments.action.library;

import static io.knotx.commons.validation.ValidationHelper.checkArgument;
import static io.knotx.fragments.api.FragmentResult.success;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.api.FragmentContext;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Inline body action factory class. It can be initialized with a configuration:
 * <pre>
 *   inlineBodyFallback {
 *     name = inline-body,
 *     config {
 *       body = "some static content"
 *       logLevel = error
 *     }
 *   }
 * </pre>
 * WARNING: This action modifies Fragment body so it should not be used in subtasks nodes.
 */
@Cacheable
public class InlineBodyActionFactory implements ActionFactory {

  private static final String ORIGINAL_BODY_KEY = "originalBody";
  private static final String BODY_KEY = "body";

  private static final String DEFAULT_EMPTY_BODY = "";

  @Override
  public String getName() {
    return "inline-body";
  }

  /**
   * Creates inline body action that replaces Fragment body with static content.
   *
   * @param alias - action alias
   * @param config - JSON configuration
   * @param vertx - vertx instance
   * @param doAction - <pre>null</pre> value expected
   */
  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    checkArgument(doAction != null, () -> new ActionConfigurationException(alias, "Inline body action does not support doAction"));

    ActionLogLevel logLevel = ActionLogLevel.fromConfig(config, ActionLogLevel.ERROR);

    return (SyncAction) fragmentContext -> {
      ActionLogger actionLogger = ActionLogger.create(alias, logLevel);

      substituteBodyInFragment(fragmentContext, config, actionLogger);

      return success(fragmentContext.getFragment(), actionLogger.toLog().toJson());
    };
  }

  private void substituteBodyInFragment(FragmentContext fragmentContext, JsonObject config,
      ActionLogger actionLogger) {
    String originalBody = fragmentContext.getFragment().getBody();
    String newBody = config.getString("body", DEFAULT_EMPTY_BODY);
    fragmentContext.getFragment()
        .setBody(newBody);
    logSubstitution(actionLogger, originalBody, newBody);
  }

  private void logSubstitution(ActionLogger actionLogger, String originalBody, String newBody) {
    actionLogger.info(ORIGINAL_BODY_KEY, originalBody);
    actionLogger.info(BODY_KEY, newBody);
  }

}
