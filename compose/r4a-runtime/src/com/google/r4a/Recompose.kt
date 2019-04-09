/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

class Recompose : Component() {
    @Children lateinit var body: @Composable() (recompose: () -> Unit) -> Unit

    // TODO(lmr): fix this before merging
    private val localRecompose: () -> Unit = { recompose() }

    override fun compose() {
        body(localRecompose)
    }
}