package com.graphhopper.routing;

import com.graphhopper.util.EdgeIteratorState;

public interface PathInterceptor {
  void handle(EdgeIteratorState edge, int index);
}
