# Task
This is a summary for all modules below.

## Modules
- [Task API](https://github.com/Knotx/knotx-fragments/tree/master/task/api) - defines a task model
- [Task Factory](https://github.com/Knotx/knotx-fragments/tree/master/task/factory) - produces a fully initialized task instance
  - [Task Factory API](https://github.com/Knotx/knotx-fragments/tree/master/task/factory/api) - the Task Factory interface 
  - [Default Task Factory](https://github.com/Knotx/knotx-fragments/tree/master/task/factory/default) - the default extendable configurable Task Factory implementation
- [Task Engine](https://github.com/Knotx/knotx-fragments/tree/master/task/engine) - evaluates tasks and modifies fragments
- [Task Handler](https://github.com/Knotx/knotx-fragments/tree/master/task/handler) - use Task Factories to produce tasks for fragments and delegates processing to Task Engine
  - [Core](https://github.com/Knotx/knotx-fragments/tree/master/task/core) - uses Task Factory to instantiate task instances, then evaluates fragments (with tasks) using Task Engine, finally notifies task execution log consumers about the results
  - Log Consumer - exposes a task execution log
    - [Log Consumer API](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/api) - the log consumer factory and serializable to JSON data model
    - [HTML Log Consumer](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/html) - wraps an HTML fragment markup with task execution debug data
    - [JSON Log Consumer](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/json) - appends task execution debug data to JSON response body
- Functional - functional tests verifying the default task factory, engine and handler

## Live demo
This live demo presents a Fragments Task configuration and [Fragments Chrome Extension](https://github.com/Knotx/knotx-fragments-chrome-extension) 
visualization capabilities.

[![Chrome Extension live demo.](assets/images/chrome-extension-live-demo.png)](https://www.youtube.com/embed/EWoHqzYGv0w)

Its written form is available [here](https://knotx.io/tutorials/chrome-extension/2_2/)

## References
- [Configurable Integrations](https://knotx.io/blog/configurable-integrations/)
