/*
 *                DO WHAT YOU WANT TO PUBLIC LICENSE
 *                     Version 3, January 2016
 *
 *  Copyright (C) 2016 Leon Fu <rtugeek@gmail.com>
 *
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *                 DO WHAT YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *      0. You just DO WHAT YOU WANT TO.
 *
 */

package com.derohimat.materialbannerx;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.derohimat.materialbannerx.adapter.MaterialPageAdapter;
import com.derohimat.materialbannerx.holder.ViewHolderCreator;
import com.derohimat.materialbannerx.view.MaterialViewPager;
import com.derohimat.materialbannerx.view.indicator.PageIndicator;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

/**
 * @author Jack Fu <rtugeek@gmail.com>
 * @date 2016/9/20
 */
public class MaterialBannerX<T> extends FrameLayout {
    private MaterialViewPager mViewPager;
    private PageIndicator mPageIndicator;
    private List mData;
    private MaterialPageAdapter mPageAdapter;
    private long autoTurningTime;
    private boolean turning;
    private boolean canTurn = false;
    private int mIndicatorMargin = 0;
    private FrameLayout mIndicatorContainer;
    private FrameLayout mCardContainer;
    private FrameLayout.LayoutParams mIndicatorParams;
    private FrameLayout.LayoutParams mCardParams;
    private boolean mIndicatorInside = false;
    private boolean mMatch = false;
    private CardView mCardView;
    private IndicatorGravity mIndicatorGravity = IndicatorGravity.LEFT;
    private AdSwitchTask adSwitchTask;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    public MaterialBannerX(Context context) {
        super(context);
        init(context);
    }

    public MaterialBannerX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MaterialBannerX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MaterialBannerX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public static int dip2Pix(Context context, float dip) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialBannerX);
        mIndicatorMargin = (int) a.getDimension(R.styleable.MaterialBannerX_indicatorMargin, dip2Pix(context, 10));
        mIndicatorGravity = IndicatorGravity.valueOf(a.getInt(R.styleable.MaterialBannerX_indicatorGravity, 0));
        mIndicatorInside = a.getBoolean(R.styleable.MaterialBannerX_indicatorInside, true);
        mMatch = a.getBoolean(R.styleable.MaterialBannerX_match, false);
        a.recycle();

        View view = LayoutInflater.from(context).inflate(R.layout.material_banner, this, true);

        mCardView = view.findViewById(R.id.card_view);
        mViewPager = view.findViewById(R.id.view_pager);
        mCardContainer = view.findViewById(R.id.container);
        mCardContainer = view.findViewById(R.id.card_container);
        mIndicatorContainer = view.findViewById(R.id.indicator_container);

        mIndicatorParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mIndicatorParams.gravity = Gravity.CENTER;

        mCardParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mCardParams.gravity = Gravity.TOP;

        //set Z value. bring indicator view to front,view.bringToFront does't work on 6.0
        ViewCompat.setZ(mCardView, 1);
        ViewCompat.setZ(mViewPager, 2);

        ViewCompat.setZ(mCardContainer, 1);
        ViewCompat.setZ(mIndicatorContainer, 2);

        updateMargin();
        setMatch(mMatch);
        adSwitchTask = new AdSwitchTask(this);

    }

    private void updateIndicatorMargin() {
        mIndicatorParams.setMargins(mIndicatorMargin, mIndicatorMargin, mIndicatorMargin, mIndicatorMargin);
        if (mPageIndicator != null) {
            mPageIndicator.getView().setLayoutParams(mIndicatorParams);
        }
    }

    public MaterialBannerX setIndicatorGravity(IndicatorGravity indicatorGravity) {
        this.mIndicatorGravity = indicatorGravity;
        mIndicatorParams.gravity = IndicatorGravity.toGravity(indicatorGravity);
        mPageIndicator.getView().setLayoutParams(mIndicatorParams);
        return this;
    }

    public MaterialBannerX setIndicator(PageIndicator pageIndicator) {
        if (mPageIndicator == pageIndicator) {
            return this;
        }

        //remove old indicator view first;
        if (mPageIndicator != null) {
            mIndicatorContainer.removeView(mPageIndicator.getView());
        }
        mPageIndicator = pageIndicator;
        mPageIndicator.setViewPager(mViewPager);
        mPageIndicator.setCurrentItem(getCurrentItem());
        mIndicatorContainer.addView(mPageIndicator.getView(), mIndicatorParams);
        //update listener
        setOnPageChangeListener(mOnPageChangeListener);
        //get the real height then update margin;
        ViewTreeObserver observer = mIndicatorContainer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateMargin();
            }
        });
        return this;
    }

    public MaterialBannerX setIndicatorMargin(int unit) {
        mIndicatorMargin = unit;
        updateIndicatorMargin();
        return this;
    }

    public boolean isIndicatorInside() {
        return mIndicatorInside;
    }

    public MaterialBannerX setIndicatorInside(boolean inside) {
        mIndicatorInside = inside;
        updateMargin();
        return this;
    }

    /**
     * update the margin value of indicator and cardContainer
     */
    private void updateMargin() {
        if (mPageIndicator == null) {
            return;
        }
        updateIndicatorMargin();
        if (!mIndicatorInside) {
            //set margin according to the mIndicatorContainer height
            mCardParams.bottomMargin = mIndicatorContainer.getHeight();
        } else {
            mCardParams.bottomMargin = 0;
        }
        mCardContainer.setLayoutParams(mCardParams);
        invalidate();
    }

    public MaterialBannerX setOnItemClickListener(OnItemClickListener onItemClickListener) {
        if (onItemClickListener == null) {
            mViewPager.setOnItemClickListener(null);
            return this;
        }
        mViewPager.setOnItemClickListener(onItemClickListener);
        return this;
    }

    public MaterialPageAdapter getAdapter() {
        return mPageAdapter;
    }

    public void setAdapter(MaterialPageAdapter adapter) {
        mPageAdapter = adapter;
        mViewPager.setAdapter(adapter);
        if (mPageIndicator != null) {
            mPageIndicator.setViewPager(mViewPager);
        }
    }

    public MaterialBannerX setPages(ViewHolderCreator holderCreator, List<T> data) {
        this.mData = data;
        mPageAdapter = new MaterialPageAdapter(holderCreator, mData);
        mViewPager.setAdapter(mPageAdapter);

        if (mPageIndicator != null) {
            mPageIndicator.setViewPager(mViewPager);
        }
        return this;
    }

    public void notifyDataSetChanged() {
        mViewPager.getAdapter().notifyDataSetChanged();
    }

    public boolean isTurning() {
        return turning;
    }

    public MaterialBannerX startTurning(long autoTurningTime) {
        if (turning) {
            stopTurning();
        }
        canTurn = true;
        this.autoTurningTime = autoTurningTime;
        turning = true;
        postDelayed(adSwitchTask, autoTurningTime);
        return this;
    }

    public void stopTurning() {
        turning = false;
        removeCallbacks(adSwitchTask);
    }

    /**
     * @param transformer
     * @return
     */
    public MaterialBannerX setTransformer(ViewPager.PageTransformer transformer) {
        mViewPager.setPageTransformer(true, transformer);
        return this;
    }

    public boolean isManualPageable() {
        return mViewPager.isCanScroll();
    }

    public void setManualPageable(boolean manualPageable) {
        mViewPager.setCanScroll(manualPageable);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            if (canTurn) {
                startTurning(autoTurningTime);
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            if (canTurn) {
                stopTurning();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public int getCurrentItem() {
        if (mViewPager != null) {
            return mViewPager.getItem();
        }
        return -1;
    }

    public void setCurrentItem(int index) {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(index);
        }
    }

    public MaterialBannerX setOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener) {
        mOnPageChangeListener = onPageChangeListener;
        if (mPageIndicator != null) {
            mPageIndicator.setOnPageChangeListener(onPageChangeListener);
            mViewPager.setOnPageChangeListener(mPageIndicator);
        } else {
            mViewPager.setOnPageChangeListener(onPageChangeListener);
        }
        return this;
    }

    public MaterialViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    public boolean isMatch() {
        return mMatch;
    }

    /**
     * whether banner match parent or not. In other word make the viewpager cover the cardView.
     *
     * @param match
     */
    public MaterialBannerX setMatch(boolean match) {
        mMatch = match;
        if (mMatch) {
            if (!mViewPager.getParent().equals(mCardContainer)) {
                mCardView.removeView(mViewPager);
                mCardContainer.addView(mViewPager, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            }
        } else {
            if (!mViewPager.getParent().equals(mCardView)) {
                mCardContainer.removeView(mViewPager);
                mCardView.addView(mViewPager, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            }
        }
        return this;
    }

    public CardView getCardView() {
        return mCardView;
    }

    public FrameLayout getIndicatorContainer() {
        return mIndicatorContainer;
    }

    public interface OnItemClickListener {

        void onItemClick(int position);
    }

    static class AdSwitchTask implements Runnable {

        private final WeakReference<MaterialBannerX> reference;

        AdSwitchTask(MaterialBannerX materialbanner) {
            this.reference = new WeakReference(materialbanner);
        }

        @Override
        public void run() {
            MaterialBannerX materialBannerX = reference.get();

            if (materialBannerX != null) {
                if (materialBannerX.mViewPager != null && materialBannerX.turning) {
                    int page = materialBannerX.mViewPager.getCurrentItem() + 1;
                    if (page >= materialBannerX.mData.size()) {
                        page = 0;
                    }
                    materialBannerX.mViewPager.setCurrentItem(page % materialBannerX.mData.size());
                    materialBannerX.postDelayed(materialBannerX.adSwitchTask, materialBannerX.autoTurningTime);
                }
            }
        }
    }


}