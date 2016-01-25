package carbon.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import carbon.Carbon;
import carbon.R;
import carbon.animation.AnimUtils;
import carbon.animation.AnimatedView;
import carbon.drawable.CircularProgressDrawable;
import carbon.drawable.ProgressBarDrawable;
import carbon.drawable.ProgressDrawable;

/**
 * Created by Marcin on 2015-02-08.
 */
public class ProgressBar extends View implements AnimatedView, TintedView {
    private ProgressDrawable drawable;

    public enum Style {
        BarDeterminate, BarIndeterminate, BarQuery, CircularDeterminate, CircularIndeterminate
    }

    public ProgressBar(Context context) {
        super(context);
        initProgressBar(null, android.R.attr.progressBarStyle);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initProgressBar(attrs, android.R.attr.progressBarStyle);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initProgressBar(attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initProgressBar(attrs, defStyleAttr);
    }

    private void initProgressBar(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyleAttr, 0);
            Style style = Style.values()[a.getInt(R.styleable.ProgressBar_carbon_progressStyle, 0)];
            if (style == Style.BarDeterminate || style == Style.BarIndeterminate || style == Style.BarQuery) {
                setDrawable(new ProgressBarDrawable());
            } else {
                setDrawable(new CircularProgressDrawable());
            }
            drawable.setStyle(style);

            drawable.setBarWidth(a.getDimension(R.styleable.ProgressBar_carbon_barWidth, 5));

            Carbon.initTint(this, attrs, defStyleAttr);
            Carbon.initAnimations(this, attrs, defStyleAttr);

            a.recycle();
        }

        if (getVisibility() == VISIBLE) {
            setBarWidth(getBarWidth() + getBarPadding());
            setBarPadding(0);
        } else {
            setBarPadding(getBarWidth() + getBarPadding());
            setBarWidth(0);
        }
    }

    public void setProgress(float progress) {
        drawable.setProgress(progress);
    }

    public float getProgress() {
        return drawable.getProgress();
    }

    public float getBarWidth() {
        return drawable.getBarWidth();
    }

    public void setBarWidth(float arcWidth) {
        drawable.setBarWidth(arcWidth);
    }

    public void setBarPadding(float padding) {
        drawable.setBarPadding(padding);
    }

    public float getBarPadding() {
        return drawable.getBarPadding();
    }

    public void setDrawable(ProgressDrawable newDrawable) {
        this.drawable = newDrawable;

        if (drawable != null)
            drawable.setCallback(null);

        if (newDrawable != null)
            newDrawable.setCallback(this);
    }

    public ProgressDrawable getDrawable() {
        return drawable;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == drawable;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (drawable != null)
            drawable.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!changed)
            return;

        if (getWidth() == 0 || getHeight() == 0)
            return;

        if (drawable != null)
            drawable.setBounds(0, 0, getWidth(), getHeight());
    }


    // -------------------------------
    // animations
    // -------------------------------

    private AnimUtils.Style inAnim, outAnim;
    private Animator animator;

    public void setVisibility(final int visibility) {
        if (visibility == View.VISIBLE && (getVisibility() != View.VISIBLE || animator != null)) {
            if (animator != null)
                animator.cancel();
            if (inAnim != AnimUtils.Style.None) {
                animator = AnimUtils.animateIn(this, inAnim, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator a) {
                        animator = null;
                        clearAnimation();
                    }
                });
            }
            super.setVisibility(visibility);
        } else if (visibility != View.VISIBLE && (getVisibility() == View.VISIBLE || animator != null)) {
            if (animator != null)
                animator.cancel();
            if (outAnim == AnimUtils.Style.None) {
                super.setVisibility(visibility);
                return;
            }
            animator = AnimUtils.animateOut(this, outAnim, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    if (((ValueAnimator) a).getAnimatedFraction() == 1)
                        ProgressBar.super.setVisibility(visibility);
                    animator = null;
                    clearAnimation();
                }
            });
        }
    }

    public void setVisibilityImmediate(final int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            setBarWidth(getBarWidth() + getBarPadding());
            setBarPadding(0);
        } else {
            setBarPadding(getBarWidth() + getBarPadding());
            setBarWidth(0);
        }
    }

    public Animator getAnimator() {
        return animator;
    }

    public AnimUtils.Style getOutAnimation() {
        return outAnim;
    }

    public void setOutAnimation(AnimUtils.Style outAnim) {
        this.outAnim = outAnim;
    }

    public AnimUtils.Style getInAnimation() {
        return inAnim;
    }

    public void setInAnimation(AnimUtils.Style inAnim) {
        this.inAnim = inAnim;
    }


    // -------------------------------
    // tint
    // -------------------------------

    ColorStateList tint;

    @Override
    public void setTint(ColorStateList list) {
        this.tint = list;
        drawable.setBarColor(list);
    }

    @Override
    public void setTint(int color) {
        setTint(ColorStateList.valueOf(color));
    }

    @Override
    public ColorStateList getTint() {
        return tint;
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
