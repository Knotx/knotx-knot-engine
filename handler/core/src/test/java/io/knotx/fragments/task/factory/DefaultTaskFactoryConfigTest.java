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

import static io.knotx.fragments.HoconLoader.verify;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class DefaultTaskFactoryConfigTest {

  @Test
  @DisplayName("Expect all node factories do not contain log level when global is not configured")
  void expectNoLogLevel(Vertx vertx) throws Throwable {
    verify("task/factory/taskFactoryWithNoGlobalLogLevel.conf", validateNoGlobalNodeLog(), vertx);
  }

  @Test
  @DisplayName("Expect all node factories contain log level when global is configured")
  void expectLogLevel(Vertx vertx) throws Throwable {
    verify("task/factory/taskFactoryWithGlobalLogLevel.conf", validateNodeLog("INFO"), vertx);
  }

  @Test
  @DisplayName("Expect local node log level is not overridden by global one")
  void expectLocalLogLevel(Vertx vertx) throws Throwable {
    verify("task/factory/taskFactoryWithLocalLogLevel.conf", validateNodeLog("ERROR"), vertx);
  }

  private Consumer<JsonObject> validateNoGlobalNodeLog() {
    return config -> {
      DefaultTaskFactoryConfig factoryConfig = new DefaultTaskFactoryConfig(config);
      factoryConfig.getNodeFactories().forEach(
          nodeFactoryOptions -> Assertions
              .assertNull(new LogLevelConfig(nodeFactoryOptions.getConfig()).getLogLevel())
      );
    };
  }

  private Consumer<JsonObject> validateNodeLog(String logLevel) {
    return config -> {
      DefaultTaskFactoryConfig factoryConfig = new DefaultTaskFactoryConfig(config);
      factoryConfig.getNodeFactories().forEach(
          nodeFactoryOptions -> Assertions
              .assertEquals(logLevel, nodeFactoryOptions.getConfig().getString("logLevel"))
      );
    };
  }
}