/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.ImageView.ScaleType;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Orientation;
import com.handmark.pulltorefresh.library.R;

@SuppressLint("ViewConstructor")
public class SimpleActivityLoadingLayout extends LoadingLayout {
	private ProgressBar mSimpleProgressBar;
	private View		mLabelsContainer;

	static final int FLIP_ANIMATION_DURATION = 150;

	public SimpleActivityLoadingLayout(Context context, final Mode mode, final Orientation scrollDirection, TypedArray attrs) {
		super(context, mode, scrollDirection, attrs);
		
		mSimpleProgressBar = (ProgressBar)mInnerLayout.findViewById(R.id.simple_progress);
		mLabelsContainer = mInnerLayout.findViewById(R.id.labelsContainer);
		mLabelsContainer.setVisibility(View.GONE);
		mSimpleProgressBar.setVisibility(View.VISIBLE);		
	}

	@Override
	protected void onLoadingDrawableSet(Drawable imageDrawable) {
		if (null != imageDrawable) {
		}
	}

	@Override
	protected void onPullImpl(float scaleOfLayout) {
		// NO-OP
	}

	@Override
	protected void pullToRefreshImpl() {
		// Only start reset Animation, we've previously show the rotate anim
		mSimpleProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected void refreshingImpl() {
		mHeaderImage.clearAnimation();
		mHeaderImage.setVisibility(View.GONE);
		mHeaderProgress.setVisibility(View.GONE);
		
		mSimpleProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected void releaseToRefreshImpl() {
	}

	@Override
	protected void resetImpl() {
		mHeaderImage.clearAnimation();
		mHeaderProgress.setVisibility(View.GONE);
		mHeaderImage.setVisibility(View.GONE);
		
		if (mSimpleProgressBar != null) {
			mSimpleProgressBar.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected int getDefaultDrawableResId() {
		return R.drawable.default_ptr_flip;
	}	
}

