/*
 * Copyright 2014 OpenMarket Ltd
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

package im.vector.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.db.MXMediasCache;

import java.util.List;

import im.vector.Matrix;
import im.vector.R;

public class VectorRoomDetailsActivity extends MXCActionBarActivity {
    private static final String LOG_TAG = "VectorAddActivity";

    // exclude the room ID
    public static final String EXTRA_ROOM_ID = "VectorRoomDetailsActivity.EXTRA_ROOM_ID";

    private String mRoomId;
    private MXMediasCache mxMediasCache;

    // define the selection section
    private int mSelectedSection = -1;

    // indexed by mSelectedSection
    private RelativeLayout mFragmentsLayout;
    private LinearLayout mTabsLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (CommonActivityUtils.shouldRestartApp()) {
            Log.e(LOG_TAG, "Restart the application.");
            CommonActivityUtils.restartApp(this);
        }

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (!intent.hasExtra(EXTRA_ROOM_ID)) {
            Log.e(LOG_TAG, "No room ID extra.");
            finish();
            return;
        }

        String matrixId = null;
        if (intent.hasExtra(EXTRA_MATRIX_ID)) {
            matrixId = intent.getStringExtra(EXTRA_MATRIX_ID);
        }

        mSession = Matrix.getInstance(getApplicationContext()).getSession(matrixId);

        if (null == mSession) {
            finish();
            return;
        }

        mRoomId = intent.getStringExtra(EXTRA_ROOM_ID);
        mRoom = mSession.getDataHandler().getRoom(mRoomId);

        setContentView(R.layout.activity_vector_room_details);

        mFragmentsLayout = (RelativeLayout)findViewById(R.id.room_details_fragments);
        mTabsLayout = (LinearLayout)findViewById(R.id.selection_tabs);

        for(int index = 0; index < mTabsLayout.getChildCount(); index++) {
            final LinearLayout sublayout = (LinearLayout)mTabsLayout.getChildAt(index);

            sublayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelectedTab(mTabsLayout.indexOfChild(v));
                }
            });
        }

        // force to hide the fragments
        // else they are displayed even if they are hidden in the layout
        for(int index = 0; index < mFragmentsLayout.getChildCount(); index++) {
            mFragmentsLayout.getChildAt(index).setVisibility(View.GONE);
        }

        onSelectedTab(0);
    }

    /**
     * Toggle a tab.
     * @param index the toggled tab.
     * @param isSelected true if the tabs is selected.
     */
    private void toggleTab(int index, boolean isSelected) {
        if (index >= 0) {
            LinearLayout subLayout = (LinearLayout)mTabsLayout.getChildAt(index);

            TextView textView = (TextView)subLayout.getChildAt(0);
            textView.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);

            View underlineView = subLayout.getChildAt(1);
            underlineView.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void onSelectedTab(int index) {
        if (index != mSelectedSection) {

            invalidateOptionsMenu();

            // hide the previous one
            if (mSelectedSection >= 0) {
                toggleTab(mSelectedSection, false);
                mFragmentsLayout.getChildAt(mSelectedSection).setVisibility(View.GONE);
            }

            mSelectedSection = index;
            toggleTab(mSelectedSection, true);
            mFragmentsLayout.getChildAt(mSelectedSection).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<android.support.v4.app.Fragment> allFragments = getSupportFragmentManager().getFragments();

        // dispatch the result to each fragments
        for (android.support.v4.app.Fragment fragment : allFragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}