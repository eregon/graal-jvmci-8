/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.graph;

import java.util.*;
import java.util.function.*;

/**
 * This class is a container of a graph that needs to be readonly and optionally a lazily created
 * mutable copy of the graph.
 */
public final class CachedGraph<G extends Graph> {

    private final G readonlyCopy;
    private G mutableCopy;

    private CachedGraph(G readonlyCopy, G mutableCopy) {
        this.readonlyCopy = readonlyCopy;
        this.mutableCopy = mutableCopy;
    }

    public static <G extends Graph> CachedGraph<G> fromReadonlyCopy(G graph) {
        return new CachedGraph<>(graph, null);
    }

    public static <G extends Graph> CachedGraph<G> fromMutableCopy(G graph) {
        return new CachedGraph<>(graph, graph);
    }

    public G getReadonlyCopy() {
        return readonlyCopy;
    }

    @SuppressWarnings("unchecked")
    public G getMutableCopy(Consumer<Map<Node, Node>> duplicationMapCallback) {
        if (mutableCopy == null) {
            mutableCopy = (G) readonlyCopy.copy(duplicationMapCallback);
        }
        return mutableCopy;
    }
}
