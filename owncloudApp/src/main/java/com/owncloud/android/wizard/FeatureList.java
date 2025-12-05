/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Christian Schabesberger
 * @author David Crespo Ríos
 * Copyright (C) 2020 Bartosz Przybylski
 * Copyright (C) 2022 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.wizard;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.R;

/**
 * @author Bartosz Przybylski
 */
public class FeatureList {

    static final private FeatureItem[] featuresList = {
            // Basic features showed on first install
            new FeatureItem("intro", R.drawable.ic_whats_new_intro, R.string.welcome_feature_1_title,
                    R.string.welcome_feature_1_text),
            new FeatureItem("desktop_uploads", R.drawable.ic_whats_new_desktop_upload, R.string.welcome_feature_2_title,
                    R.string.welcome_feature_2_text, R.drawable.ic_desktop_app),
            new FeatureItem("file_deduplication", R.drawable.ic_whats_new_file_deduplication, R.string.welcome_feature_3_title,
                    R.string.welcome_feature_3_text),
            new FeatureItem("search", R.drawable.ic_whats_new_search, R.string.welcome_feature_4_title,
                    R.string.welcome_feature_4_text),
            new FeatureItem("sharing", R.drawable.ic_whats_new_sharing, R.string.welcome_feature_5_title,
                    R.string.welcome_feature_5_text),
    };

    static public FeatureItem[] get() {
        return featuresList;
    }

    static public class FeatureItem implements Parcelable {
        private static final int DO_NOT_SHOW = -1;
        private final int image;
        private final int titleText;
        private final int contentText;
        private final String id;

        private final int extraImage;

        private FeatureItem(String id, int image, int titleText, int contentText) {
            this(id, image, titleText, contentText, DO_NOT_SHOW);
        }

        private FeatureItem(String id, int image, int titleText, int contentText, int extraImage) {
            this.image = image;
            this.titleText = titleText;
            this.contentText = contentText;
            this.id = id;
            this.extraImage = extraImage;
        }

        public String getId() {
            return id;
        }

        public boolean shouldShowImage() {
            return image != DO_NOT_SHOW;
        }

        public int getImage() {
            return image;
        }

        public boolean shouldShowTitleText() {
            return titleText != DO_NOT_SHOW;
        }

        public int getTitleText() {
            return titleText;
        }

        public boolean shouldShowContentText() {
            return contentText != DO_NOT_SHOW;
        }

        public int getContentText() {
            return contentText;
        }

        public int getExtraImage() {
            return extraImage;
        }

        public boolean shouldShowExtraImage() {
            return extraImage != DO_NOT_SHOW;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeInt(image);
            dest.writeInt(titleText);
            dest.writeInt(contentText);
            dest.writeInt(extraImage);
        }

        private FeatureItem(Parcel p) {
            id = p.readString();
            image = p.readInt();
            titleText = p.readInt();
            contentText = p.readInt();
            extraImage = p.readInt();
        }

        public static final Parcelable.Creator CREATOR =
                new Parcelable.Creator() {

                    @Override
                    public Object createFromParcel(Parcel source) {
                        return new FeatureItem(source);
                    }

                    @Override
                    public Object[] newArray(int size) {
                        return new FeatureItem[size];
                    }
                };
    }
}
