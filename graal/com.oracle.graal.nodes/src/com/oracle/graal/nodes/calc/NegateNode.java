/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.calc;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.compiler.common.type.ArithmeticOpTable.UnaryOp;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.lir.gen.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

/**
 * The {@code NegateNode} node negates its operand.
 */
@NodeInfo
public class NegateNode extends UnaryNode implements ArithmeticLIRLowerable, NarrowableArithmeticNode {

    private final UnaryOp op;

    @Override
    public boolean inferStamp() {
        return updateStamp(op.foldStamp(getValue().stamp()));
    }

    /**
     * Creates new NegateNode instance.
     *
     * @param value the instruction producing the value that is input to this instruction
     */
    public static NegateNode create(ValueNode value) {
        return create(ArithmeticOpTable.forStamp(value.stamp()).getNeg(), value);
    }

    public static NegateNode create(UnaryOp op, ValueNode value) {
        return USE_GENERATED_NODES ? new NegateNodeGen(op, value) : new NegateNode(op, value);
    }

    protected NegateNode(UnaryOp op, ValueNode value) {
        super(op.foldStamp(value.stamp()), value);
        this.op = op;
    }

    public Constant evalConst(Constant... inputs) {
        assert inputs.length == 1;
        return op.foldConstant(inputs[0]);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        if (forValue.isConstant()) {
            return ConstantNode.forPrimitive(stamp(), op.foldConstant(forValue.asConstant()));
        }
        if (forValue instanceof NegateNode) {
            return ((NegateNode) forValue).getValue();
        }
        if (forValue instanceof SubNode) {
            SubNode sub = (SubNode) forValue;
            return SubNode.create(sub.getY(), sub.getX());
        }
        return this;
    }

    @Override
    public void generate(NodeMappableLIRBuilder builder, ArithmeticLIRGenerator gen) {
        builder.setResult(this, gen.emitNegate(builder.operand(getValue())));
    }
}
