/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.adapters;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import org.matrix.androidsdk.rest.model.RoomMember;

import java.util.Comparator;

import im.vector.contacts.Contact;


public class ParticipantAdapterItem {
    // displayed info
    public String mDisplayName;
    public String mAvatarUrl;
    public Bitmap mAvatarBitmap;

    // user id
    public String mUserId;

    // the data is extracted either from a room member or a contact
    public RoomMember mRoomMember;
    public Contact mContact;

    // search fields
    private String mLowerCaseDisplayName;
    private String mLowerCaseMatrixId;

    public ParticipantAdapterItem(RoomMember member) {
        mDisplayName = member.getName();
        mAvatarUrl = member.avatarUrl;
        mUserId = member.getUserId();

        mRoomMember = member;
        mContact = null;

        initSearchByPatternFields();
    }

    public ParticipantAdapterItem(Contact contact, Context context) {
        mDisplayName = contact.getDisplayName();
        mAvatarBitmap = contact.getThumbnail(context);

        mUserId = null;
        mRoomMember = null;

        mContact = contact;

        initSearchByPatternFields();
    }

    public ParticipantAdapterItem(String displayName, String avatarUrl, String userId) {
        mDisplayName = displayName;
        mAvatarUrl = avatarUrl;
        mUserId = userId;

        initSearchByPatternFields();
    }

    /**
     * Init the search by pattern fields
     */
    private void initSearchByPatternFields() {
        if (!TextUtils.isEmpty(mDisplayName)) {
            mLowerCaseDisplayName = mDisplayName.toLowerCase();
        }

        if (!TextUtils.isEmpty(mUserId)) {

            int sepPos = mUserId.indexOf(":");

            if (sepPos > 0) {
                mLowerCaseMatrixId = mUserId.substring(0, sepPos).toLowerCase();
            }
        }
    }

    // Comparator to order members alphabetically
    public static Comparator<ParticipantAdapterItem> alphaComparator = new Comparator<ParticipantAdapterItem>() {
        @Override
        public int compare(ParticipantAdapterItem part1, ParticipantAdapterItem part2) {
            String lhs = TextUtils.isEmpty(part1.mDisplayName) ? part1.mUserId : part1.mDisplayName;
            String rhs = TextUtils.isEmpty(part2.mDisplayName) ? part2.mUserId : part2.mDisplayName;

            if (lhs == null) {
                return -1;
            }
            else if (rhs == null) {
                return 1;
            }
            
            /*
            // disable to have the same sort order than the IOS client.
            if (lhs.startsWith("@")) {
                lhs = lhs.substring(1);
            }
            if (rhs.startsWith("@")) {
                rhs = rhs.substring(1);
            }*/
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    };

    /**
     * Test if a room member matches with a pattern.
     * The check is done with the displayname and the userId.
     * @param aPattern the pattern to search.
     * @return true if it matches.
     */
    public boolean matchWithPattern(String aPattern) {
        if (TextUtils.isEmpty(aPattern)) {
            return false;
        }

        boolean res = false;

        if (!res && !TextUtils.isEmpty(mLowerCaseDisplayName)) {
            res = mLowerCaseDisplayName.indexOf(aPattern) >= 0;
        }

        if (!res && !TextUtils.isEmpty(mLowerCaseMatrixId)) {
            res = mLowerCaseMatrixId.indexOf(aPattern) >= 0;
        }

        // the room member class only checks the matrixId and the displayname
        // avoid testing twice
        /*if (!res && (null != mRoomMember)) {
            res = mRoomMember.matchWithPattern(aPattern);
        }*/

        if (!res && (null != mContact)) {
            res = mContact.matchWithPattern(aPattern);
        }

        return res;
    }

    /**
     * Test if a room member fields matches with a regex
     * The check is done with the displayname and the userId.
     * @param aRegEx the pattern to search.
     * @return true if it matches.
     */
    public boolean matchWithRegEx(String aRegEx) {

        if (TextUtils.isEmpty(aRegEx)) {
            return false;
        }

        boolean res = false;

        if (!res && !TextUtils.isEmpty(mDisplayName)) {
            res = mDisplayName.matches(aRegEx);
        }

        if (!res && !TextUtils.isEmpty(mUserId)) {
            res = mUserId.matches(aRegEx);
        }

        if (!res && (null != mRoomMember)) {
            res = mRoomMember.matchWithRegEx(aRegEx);
        }

        if (!res && (null != mContact)) {
            res = mContact.matchWithRegEx(aRegEx);
        }

        return res;
    }
}