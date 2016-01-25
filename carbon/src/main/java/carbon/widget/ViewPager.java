package carbon.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.nineoldandroids.view.ViewHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import carbon.Carbon;
import carbon.R;
import carbon.drawable.DefaultColorStateList;
import carbon.drawable.EdgeEffect;
import carbon.drawable.RectDrawable;

/**
 * Created by Marcin on 2015-02-28.
 */
public class ViewPager extends android.support.v4.view.ViewPager implements TintedView {
    private int mTouchSlop;
    EdgeEffect leftGlow;
    EdgeEffect rightGlow;
    private boolean drag = true;
    private float prevX;
    private int overscrollMode;

    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;

    private final OnPageChangeListener internalOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            for (OnPageChangeListener listener : pageChangeListenerList)
                listener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            for (OnPageChangeListener listener : pageChangeListenerList)
                listener.onPageSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            for (OnPageChangeListener listener : pageChangeListenerList)
                listener.onPageScrollStateChanged(state);
        }
    };
    List<OnPageChangeListener> pageChangeListenerList = new ArrayList<>();

    public void addOnPageChangeListener(OnPageChangeListener listener) {
        pageChangeListenerList.add(listener);
    }

    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        pageChangeListenerList.remove(listener);
    }

    /**
     * @param listener page changed listener
     * @deprecated Use {@link #addOnPageChangeListener(OnPageChangeListener)} instead. That method
     * allows to add more than one listener.
     */
    @Deprecated
    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        pageChangeListenerList.clear();
        pageChangeListenerList.add(listener);
    }

    public ViewPager(Context context) {
        super(context, null);
        initViewPager(null, R.attr.carbon_viewPagerStyle);
    }

    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewPager(attrs, R.attr.carbon_viewPagerStyle);
    }

    public ViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        initViewPager(attrs, defStyleAttr);
    }

    private void initViewPager(AttributeSet attrs, int defStyleAttr) {
        super.setOnPageChangeListener(internalOnPageChangeListener);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ViewPager, defStyleAttr, 0);
        for (int i = 0; i < a.getIndexCount(); i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.ViewPager_carbon_overScroll) {
                setOverScrollMode(a.getInt(attr, OVER_SCROLL_ALWAYS));
            }
        }
        a.recycle();

        Carbon.initTint(this, attrs, defStyleAttr);
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() == 0) {
            return getWidth();
        }
        if (getChildCount() > 0 && getAdapter() != null) {
            if (getCurrentItem() == getAdapter().getCount() - 1) {
                View child = getChildAt(getChildCount() - 1);
                scrollRange = Math.max(0,
                        child.getRight() - (getWidth() - getPaddingRight() - getPaddingLeft()));
            } else {
                scrollRange = Integer.MAX_VALUE;
            }
        }
        return scrollRange;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (leftGlow != null) {
            final int scrollX = getScrollX();
            if (!leftGlow.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(270);
                canvas.translate(-height + getPaddingTop(), Math.min(0, scrollX));
                leftGlow.setSize(height, getWidth());
                if (leftGlow.draw(canvas)) {
                    postInvalidate();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!rightGlow.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(90);
                canvas.translate(-getPaddingTop(),
                        -(Math.max(getScrollRange(), scrollX) + width));
                rightGlow.setSize(height, width);
                if (rightGlow.draw(canvas)) {
                    postInvalidate();
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float deltaX = prevX - ev.getX();

                if (!drag && Math.abs(deltaX) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    drag = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }
                if (drag) {
                    final int oldX = getScrollX();
                    final int range = getScrollRange();
                    boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                            (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    if (canOverscroll) {
                        float pulledToX = oldX + deltaX;
                        if (pulledToX < 0) {
                            leftGlow.onPull(deltaX / getWidth(), 1.f - ev.getY() / getHeight());
                            if (!rightGlow.isFinished())
                                rightGlow.onRelease();
                        } else if (pulledToX > range) {
                            rightGlow.onPull(deltaX / getWidth(), ev.getY() / getHeight());
                            if (!leftGlow.isFinished())
                                leftGlow.onRelease();
                        }
                        if (leftGlow != null && (!leftGlow.isFinished() || !rightGlow.isFinished()))
                            postInvalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drag) {
                    drag = false;

                    if (leftGlow != null) {
                        leftGlow.onRelease();
                        rightGlow.onRelease();
                    }
                }
                break;
        }
        prevX = ev.getX();

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (mode != OVER_SCROLL_NEVER) {
            if (leftGlow == null) {
                Context context = getContext();
                leftGlow = new EdgeEffect(context);
                rightGlow = new EdgeEffect(context);
                updateTint();
            }
        } else {
            leftGlow = null;
            rightGlow = null;
        }
        try {
            super.setOverScrollMode(OVER_SCROLL_NEVER);
        } catch (Exception e) {
            // Froyo
        }
        this.overscrollMode = mode;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTint();
    }

    // -------------------------------
    // tint
    // -------------------------------

    ColorStateList tint;

    @Override
    public void setTint(ColorStateList list) {
        this.tint = list;
        updateTint();
    }

    @Override
    public void setTint(int color) {
        if (color == 0) {
            setTint(new DefaultColorStateList(getContext()));
        } else {
            setTint(ColorStateList.valueOf(color));
        }
    }

    @Override
    public ColorStateList getTint() {
        return tint;
    }

    private void updateTint() {
        if (tint == null)
            return;
        int color = tint.getColorForState(getDrawableState(), tint.getDefaultColor());
        if (leftGlow != null)
            leftGlow.setColor(color);
        if (rightGlow != null)
            rightGlow.setColor(color);
    }


    // -------------------------------
    // scroll bars
    // -------------------------------

    Drawable scrollBarDrawable;

    protected void onDrawHorizontalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        if (scrollBarDrawable == null) {
            Class<? extends Drawable> scrollBarClass = scrollBar.getClass();
            try {
                Field mVerticalThumbField = scrollBarClass.getDeclaredField("mHorizontalThumb");
                mVerticalThumbField.setAccessible(true);
                scrollBarDrawable = new RectDrawable(Carbon.getThemeColor(getContext(), R.attr.colorPrimary));
                mVerticalThumbField.set(scrollBar, scrollBarDrawable);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        if (scrollBarDrawable == null) {
            Class<? extends Drawable> scrollBarClass = scrollBar.getClass();
            try {
                Field mVerticalThumbField = scrollBarClass.getDeclaredField("mVerticalThumb");
                mVerticalThumbField.setAccessible(true);
                scrollBarDrawable = new RectDrawable(Carbon.getThemeColor(getContext(), R.attr.colorPrimary));
                mVerticalThumbField.set(scrollBar, scrollBarDrawable);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }


    // -------------------------------
    // transformations  // TODO: NineOldAndroids could be inlined here
    // -------------------------------

    public void setAlpha(float x) {
        ViewHelper.setAlpha(this, x);
    }

    public void setTranslationX(float x) {
        ViewHelper.setTranslationX(this, x);
    }

    public void setTranslationY(float y) {
        ViewHelper.setTranslationY(this, y);
    }

    public void setX(float x) {
        ViewHelper.setX(this, x);
    }

    public void setY(float y) {
        ViewHelper.setY(this, y);
    }

    public void setScaleX(float x) {
        ViewHelper.setScaleX(this, x);
    }

    public void setScaleY(float y) {
        ViewHelper.setScaleY(this, y);
    }

    public void setScrollX(int x) {
        ViewHelper.setScrollX(this, x);
    }

    public void setScrollY(int y) {
        ViewHelper.setScrollY(this, y);
    }

    public void setPivotX(float x) {
        ViewHelper.setPivotX(this, x);
    }

    public void setPivotY(float y) {
        ViewHelper.setPivotY(this, y);
    }

    public void setRotationX(float x) {
        ViewHelper.setRotationX(this, x);
    }

    public void setRotationY(float y) {
        ViewHelper.setRotationY(this, y);
    }

    public void setRotation(float y) {
        ViewHelper.setRotation(this, y);
    }
}
