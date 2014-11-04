/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.phases.common.inlining.walker;

import com.oracle.graal.api.meta.ResolvedJavaMethod;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.graph.FixedNodeProbabilityCache;

import java.util.*;
import java.util.function.ToDoubleFunction;

import static com.oracle.graal.compiler.common.GraalOptions.CapInheritedRelevance;

/**
 * <p>
 * A {@link CallsiteHolder} whose graph has been copied already and thus can be modified without
 * affecting the original (usually cached) version.
 * </p>
 *
 * <p>
 * An instance of this class is derived from an
 * {@link com.oracle.graal.phases.common.inlining.info.elem.InlineableGraph InlineableGraph} and
 * contains a subset of the information there: just the {@link Invoke} nodes from it. Such nodes are
 * candidates for depth-first search of further inlining opportunities (thus the adjective
 * "explorable" given to this class)
 * </p>
 *
 * @see InliningData#moveForward()
 */
public final class CallsiteHolderExplorable extends CallsiteHolder {

    /**
     * Graph in which inlining may be performed at one or more of the callsites containined in
     * {@link #remainingInvokes}.
     */
    private final StructuredGraph graph;

    private final LinkedList<Invoke> remainingInvokes;
    private final double probability;
    private final double relevance;

    /**
     * @see #getFixedParams()
     */
    private final Set<ParameterNode> fixedParams;

    private final ToDoubleFunction<FixedNode> probabilities;
    private final ComputeInliningRelevance computeInliningRelevance;

    public CallsiteHolderExplorable(StructuredGraph graph, double probability, double relevance, BitSet freshlyInstantiatedArguments) {
        assert graph != null;
        this.graph = graph;
        this.probability = probability;
        this.relevance = relevance;
        this.fixedParams = fixedParamsAt(freshlyInstantiatedArguments);
        remainingInvokes = new InliningIterator(graph).apply();
        if (remainingInvokes.isEmpty()) {
            probabilities = null;
            computeInliningRelevance = null;
        } else {
            probabilities = new FixedNodeProbabilityCache();
            computeInliningRelevance = new ComputeInliningRelevance(graph, probabilities);
            computeProbabilities();
        }
        assert repOK();
    }

    /**
     * @see #getFixedParams()
     */
    @SuppressWarnings("unchecked")
    private Set<ParameterNode> fixedParamsAt(BitSet freshlyInstantiatedArguments) {
        if (freshlyInstantiatedArguments == null || freshlyInstantiatedArguments.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set<ParameterNode> result = new HashSet<>();
        for (ParameterNode p : graph.getNodes(ParameterNode.class)) {
            if (freshlyInstantiatedArguments.get(p.index())) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * <p>
     * Parameters for which the callsite targeting {@link #graph()} provides "fixed" arguments. That
     * callsite isn't referenced by this instance. Instead, it belongs to the graph of the caller of
     * this {@link CallsiteHolderExplorable}
     * </p>
     *
     * <p>
     * Constant arguments don't contribute to fixed-params: those params have been removed already,
     * see {@link com.oracle.graal.phases.common.inlining.info.elem.InlineableGraph}.
     * </p>
     *
     * <p>
     * Instead, fixed-params are those receiving freshly instantiated arguments (possibly
     * instantiated several levels up in the call-hierarchy)
     * </p>
     * */
    public Set<ParameterNode> getFixedParams() {
        return fixedParams;
    }

    public boolean repOK() {
        for (Invoke invoke : remainingInvokes) {
            if (!invoke.asNode().isAlive() || !containsInvoke(invoke)) {
                assert false;
                return false;
            }
            if (!allArgsNonNull(invoke)) {
                assert false;
                return false;
            }
        }
        for (ParameterNode fixedParam : fixedParams) {
            if (!containsParam(fixedParam)) {
                assert false;
                return false;
            }
        }
        return true;
    }

    @Override
    public ResolvedJavaMethod method() {
        return graph == null ? null : graph.method();
    }

    @Override
    public boolean hasRemainingInvokes() {
        return !remainingInvokes.isEmpty();
    }

    @Override
    public StructuredGraph graph() {
        return graph;
    }

    public Invoke popInvoke() {
        return remainingInvokes.removeFirst();
    }

    public void pushInvoke(Invoke invoke) {
        remainingInvokes.push(invoke);
    }

    public static boolean allArgsNonNull(Invoke invoke) {
        for (ValueNode arg : invoke.callTarget().arguments()) {
            if (arg == null) {
                assert false;
                return false;
            }
        }
        return true;
    }

    public boolean containsInvoke(Invoke invoke) {
        for (Invoke i : graph().getInvokes()) {
            if (i == invoke) {
                return true;
            }
        }
        return false;
    }

    public boolean containsParam(ParameterNode param) {
        for (ParameterNode p : graph.getNodes(ParameterNode.class)) {
            if (p == param) {
                return true;
            }
        }
        return false;
    }

    public void computeProbabilities() {
        computeInliningRelevance.compute();
    }

    public double invokeProbability(Invoke invoke) {
        return probability * probabilities.applyAsDouble(invoke.asNode());
    }

    public double invokeRelevance(Invoke invoke) {
        return Math.min(CapInheritedRelevance.getValue(), relevance) * computeInliningRelevance.getRelevance(invoke);
    }

    @Override
    public String toString() {
        return (graph != null ? method().format("%H.%n(%p)") : "<null method>") + remainingInvokes;
    }
}