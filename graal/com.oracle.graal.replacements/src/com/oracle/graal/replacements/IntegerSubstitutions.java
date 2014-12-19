/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.replacements;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.replacements.nodes.*;

@ClassSubstitution(Integer.class)
public class IntegerSubstitutions {

    @MethodSubstitution
    public static int reverseBytes(int i) {
        return ReverseBytesNode.reverse(i);
    }

    @MethodSubstitution
    public static int numberOfLeadingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        return 31 - BitScanReverseNode.unsafeScan(i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        return BitScanForwardNode.unsafeScan(i);
    }

    @MethodSubstitution
    public static int bitCount(int i) {
        return BitCountNode.bitCount(i);
    }

    @MethodSubstitution
    public static int divideUnsigned(int dividend, int divisor) {
        return UnsignedDivNode.unsignedDivide(dividend, divisor);
    }

    @MethodSubstitution
    public static int remainderUnsigned(int dividend, int divisor) {
        return UnsignedRemNode.unsignedRemainder(dividend, divisor);
    }
}