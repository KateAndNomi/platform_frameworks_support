/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.view.inputmethod;

import static org.junit.Assert.assertArrayEquals;

import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.inputmethod.EditorInfo;

import androidx.core.app.TestActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class EditorInfoCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final String CONTENT_MIME_TYPES_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";
    private static final String CONTENT_MIME_TYPES_INTEROP_KEY =
            "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";

    public EditorInfoCompatTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRoundTrip() {
        EditorInfo editorInfo1 = new EditorInfo();
        String[] mimeTypes1 = new String[]{"image/gif", "image/jpeg", "image/png"};
        EditorInfoCompat.setContentMimeTypes(editorInfo1, mimeTypes1);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo1), mimeTypes1);

        EditorInfo editorInfo2 = new EditorInfo();
        String[] mimeTypes2 = new String[]{"image/gif", "image/jpeg"};
        EditorInfoCompat.setContentMimeTypes(editorInfo2, mimeTypes2);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo2), mimeTypes2);

        EditorInfo editorInfo3 = new EditorInfo();
        String[] mimeTypes3 = new String[]{};
        EditorInfoCompat.setContentMimeTypes(editorInfo3, mimeTypes3);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo3), mimeTypes3);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void testRoundTripInteropOnlyNew() {
        EditorInfo editorInfo = new EditorInfo();
        String[] mimeTypes = new String[]{"image/gif", "image/jpeg", "image/png"};

        if (editorInfo.extras == null) {
            editorInfo.extras = new Bundle();
        }
        editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, mimeTypes);

        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo), mimeTypes);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void testRoundTripInteropOnlyOld() {
        EditorInfo editorInfo = new EditorInfo();
        String[] mimeTypes = new String[]{"image/gif", "image/jpeg", "image/png"};

        if (editorInfo.extras == null) {
            editorInfo.extras = new Bundle();
        }
        editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_INTEROP_KEY, mimeTypes);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo), mimeTypes);
    }
}
