/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpTest {

    @Test
    fun constructor() {
        val dim1 = Dp(dp = 5f)
        assertEquals(5f, dim1.dp, 0f)

        val dim2 = Dp(dp = Float.POSITIVE_INFINITY)
        assertEquals(Float.POSITIVE_INFINITY, dim2.dp, 0f)

        val dim3 = Dp(dp = Float.NaN)
        assertEquals(Float.NaN, dim3.dp, 0f)
    }

    @Test
    fun dpIntegerConstruction() {
        val dim = 10.dp
        assertEquals(10f, dim.dp, 0f)
    }

    @Test
    fun dpFloatConstruction() {
        val dim = 10f.dp
        assertEquals(10f, dim.dp, 0f)
    }

    @Test
    fun dpDoubleConstruction() {
        val dim = 10.0.dp
        assertEquals(10f, dim.dp, 0f)
    }

    @Test
    fun subtractOperator() {
        assertEquals(-1f, (3.dp - 4.dp).dp)
        assertEquals(1f, (10.dp - 9.dp).dp, 0f)
    }

    @Test
    fun addOperator() {
        assertEquals(2f, (1.dp + 1.dp).dp, 0f)
        assertEquals(10f, (6.dp + 4.dp).dp, 0f)
    }

    @Test
    fun multiplyOperator() {
        assertEquals(0f, (1.dp * 0f).dp, 0f)
        assertEquals(10f, (1.dp * 10f).dp, 0f)
    }

    @Test
    fun multiplyOperatorScalar() {
        assertEquals(10f, 10f * 1.dp.dp, 0f)
        assertEquals(10f, 10 * 1.dp.dp, 0f)
        assertEquals(10f, (10.0 * 1.dp).dp, 0f)
    }

    @Test
    fun multiplyDimension() {
        assertEquals(DpSquared(40f), 10.dp * 4.dp)
    }

    @Test
    fun multiplyDimensionSquared() {
        assertEquals(DpCubed(40f), 10.dp * (2.dp * 2.dp))
    }

    @Test
    fun divideOperator() {
        assertEquals(10f, 100.dp / 10f.dp, 0f)
        assertEquals(0f, 0.dp / 10f.dp, 0f)
    }

    @Test
    fun divideOperatorInverse() {
        assertEquals(DpInverse(10f), 100f / 10.dp)
        assertEquals(DpInverse(10f), 100.0 / 10.dp)
        assertEquals(DpInverse(10f), 100 / 10.dp)
    }

    @Test
    fun divideToScalar() {
        assertEquals(1f, 1.dp / 1.dp, 0f)
    }

    @Test
    fun divideToInverse() {
        assertEquals(DpInverse(10f), 100.dp / (5.dp * 2.dp))
    }

    @Test
    fun hairline() {
        assertEquals(0f, Dp.Hairline.dp, 0f)
    }

    @Test
    fun infinite() {
        assertEquals(Float.POSITIVE_INFINITY, Dp.Infinity.dp, 0f)
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun compare() {
        assertTrue(0.dp < Float.MIN_VALUE.dp)
        assertTrue(1.dp < 3.dp)
        assertEquals(0, 1.dp.compareTo(1.dp))
        assertTrue(1.dp > 0.dp)
        assertTrue(Float.NEGATIVE_INFINITY.dp < Dp.Infinity)
        assertTrue(Float.NEGATIVE_INFINITY.dp < 0.dp)
        assertTrue(Dp.Infinity > Float.MAX_VALUE.dp)

        val zeroNaN = 0f / 0f
        val infNaN = Float.POSITIVE_INFINITY / Float.NEGATIVE_INFINITY
        assertEquals(0, zeroNaN.dp.compareTo(zeroNaN.dp))
        assertEquals(0, infNaN.dp.compareTo(infNaN.dp))
    }

    @Test
    fun addDimension2() {
        assertEquals(DpSquared(4f), (2.dp * 1.dp) + (1.dp * 2.dp))
    }

    @Test
    fun subtractDimension2() {
        assertEquals(DpSquared(0f), (2.dp * 3.dp) - (3.dp * 2.dp))
    }

    @Test
    fun divideDimension2() {
        assertEquals(DpSquared(1f), (2.dp * 5.dp) / 10f)
    }

    @Test
    fun divideDimension2Dimension() {
        assertEquals(1f, ((2.dp * 2.dp) / 4.dp).dp, 0f)
    }

    @Test
    fun divideDimension2Dimension2() {
        assertEquals(1f, (2.dp * 2.dp) / (2.dp * 2.dp))
    }

    @Test
    fun divideDimension2Dimension3() {
        assertEquals(DpInverse(0.5f), (2.dp * 2.dp) / (2.dp * 2.dp * 2.dp))
    }

    @Test
    fun multiplyDimension2() {
        assertEquals(DpSquared(4f), (2.dp * 1.dp) * 2f)
    }

    @Test
    fun multiplyDimension2Dimension() {
        assertEquals(DpCubed(4f), (2.dp * 1.dp) * 2.dp)
    }

    @Test
    fun compareDimension2() {
        assertTrue(DpSquared(0f) < DpSquared(Float.MIN_VALUE))
        assertTrue(DpSquared(1f) < DpSquared(3f))
        assertTrue(DpSquared(1f) == DpSquared(1f))
        assertTrue(DpSquared(1f) > DpSquared(0f))
    }

    @Test
    fun addDimension3() {
        assertEquals(DpCubed(4f), (2.dp * 1.dp * 1.dp) + (1.dp * 2.dp * 1.dp))
    }

    @Test
    fun subtractDimension3() {
        assertEquals(DpCubed(0f), (2.dp * 3.dp * 1.dp) - (3.dp * 2.dp * 1.dp))
    }

    @Test
    fun divideDimension3() {
        assertEquals(DpCubed(1f), (2.dp * 5.dp * 1.dp) / 10f)
    }

    @Test
    fun divideDimension3Dimension() {
        assertEquals(DpSquared(1f), (2.dp * 2.dp * 1.dp) / 4.dp)
    }

    @Test
    fun divideDimension3Dimension2() {
        assertEquals(1f, ((2.dp * 2.dp * 1.dp) / (2.dp * 2.dp)).dp, 0f)
    }

    @Test
    fun divideDimension3Dimension3() {
        assertEquals(1f, (2.dp * 2.dp * 1.dp) / (2.dp * 2.dp * 1.dp))
    }

    @Test
    fun multiplyDimension3() {
        assertEquals(DpCubed(4f), (2.dp * 1.dp * 1.dp) * 2f)
    }

    @Test
    fun compareDimension3() {
        assertTrue(DpCubed(0f) < DpCubed(Float.MIN_VALUE))
        assertTrue(DpCubed(1f) < DpCubed(3f))
        assertTrue(DpCubed(1f) == DpCubed(1f))
        assertTrue(DpCubed(1f) > DpCubed(0f))
    }

    @Test
    fun addDimensionInverse() {
        assertEquals(DpInverse(1f), 1 / 2.dp + 1 / 2.dp)
    }

    @Test
    fun subtractDimensionInverse() {
        assertEquals(DpInverse(0f), 1 / 2.dp - 1 / 2.dp)
    }

    @Test
    fun divideDimensionInverse() {
        assertEquals(DpInverse(1f), (10 / 1.dp) / 10f)
    }

    @Test
    fun multiplyDimensionInverse() {
        assertEquals(DpInverse(4f), (1 / 2.dp) * 8f)
    }

    @Test
    fun multiplyDimensionInverseDimension() {
        assertEquals(4f, (1 / 2.dp) * 8.dp)
    }

    @Test
    fun multiplyDimensionInverseDimension2() {
        assertEquals(4f, ((1 / 2.dp) * (8.dp * 1.dp)).dp, 0f)
    }

    @Test
    fun multiplyDimensionInverseDimension3() {
        assertEquals(DpSquared(4f), (1 / 2.dp) * (8.dp * 1.dp * 1.dp))
    }

    @Test
    fun compareDimensionInverse() {
        assertTrue(DpInverse(0f) < DpInverse(Float.MIN_VALUE))
        assertTrue(DpInverse(1f) < DpInverse(3f))
        assertTrue(DpInverse(1f) == DpInverse(1f))
        assertTrue(DpInverse(1f) > DpInverse(0f))
    }

    @Test
    fun minTest() {
        assertEquals(10f, min(10.dp, 20.dp).dp, 0f)
        assertEquals(10f, min(20.dp, 10.dp).dp, 0f)
        assertEquals(10f, min(10.dp, 10.dp).dp, 0f)
    }

    @Test
    fun maxTest() {
        assertEquals(20f, max(10.dp, 20.dp).dp, 0f)
        assertEquals(20f, max(20.dp, 10.dp).dp, 0f)
        assertEquals(20f, max(20.dp, 20.dp).dp, 0f)
    }

    @Test
    fun coerceIn() {
        assertEquals(10f, 10.dp.coerceIn(0.dp, 20.dp).dp, 0f)
        assertEquals(10f, 20.dp.coerceIn(0.dp, 10.dp).dp, 0f)
        assertEquals(10f, 0.dp.coerceIn(10.dp, 20.dp).dp, 0f)
        try {
            10.dp.coerceIn(20.dp, 10.dp)
            fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        assertEquals(10f, 0.dp.coerceAtLeast(10.dp).dp, 0f)
        assertEquals(10f, 10.dp.coerceAtLeast(5.dp).dp, 0f)
        assertEquals(10f, 10.dp.coerceAtLeast(10.dp).dp, 0f)
    }

    @Test
    fun coerceAtMost() {
        assertEquals(10f, 100.dp.coerceAtMost(10.dp).dp, 0f)
        assertEquals(10f, 10.dp.coerceAtMost(20.dp).dp, 0f)
        assertEquals(10f, 10.dp.coerceAtMost(10.dp).dp, 0f)
    }
}