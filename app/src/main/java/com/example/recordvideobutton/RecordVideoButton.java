package com.example.recordvideobutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * create by hmSong on 2021/8/10
 */
public class RecordVideoButton extends View {
    //最外侧的圆环
    private Paint mOutCirclePaint;
    private int mOutCircleColor = Color.WHITE;
    private float outCirclePaintWidth = 10;
    private float mOutCircleWidth;
    private float mOutCircleRadius;
    //里面圆角矩形
    private Paint mInnerRectPaint;
    private int innerRectColor = 0xFFEA7172;
    private final RectF mRectF = new RectF();
    private float rectWidth;
    private float corner;
    private float mMinCorner;
    private float mMaxCorner;
    private float mMinRectWidth;
    private float mMaxRectWidth;
    //圆角矩形外的圆
    private Paint mInnerBackgroundCirclePaint;
    private float mInnerBackgroundRadius;
    private float mMaxInnerBackgroundRadius;
    private float mMInInnerBackgroundRadius;
    private int mInnerBackgroundCircleColor = Color.WHITE;
    private int alpha = 255;
    private final int maxAlpha = 255;
    private final int minAlpha = 0;

    //最外面的背景圆
    private Paint mOutBackgroundCirclePaint;
    private float mOutBackgroundRadius;
    private float mMaxOutBackgroundCircleRadius;
    private float mMiddleOutBackgroundCircleRadius;//暂停时的大小
    private float mMinOutBackgroundCircleRadius;
    private int mOutBackgroundCircleColor = 0x66FFFFFF;

    private float innerRectMaxDividerWidth = 15;//最大时外面距离圆环的宽度

    private RecordMode mRecordMode = RecordMode.SINGLE_CLICK;
    private RecordState mRecordState = RecordState.ORIGIN;
    private final Handler mHandler = new Handler();
    private OnRecordStateChangedListener mOnRecordStateChangedListener;
    private OnCaptureClickListener onCaptureClickListener;
    private final ClickRunnable mClickRunnable = new ClickRunnable();

    private final AnimatorSet mBeginAnimatorSet = new AnimatorSet();
    private final AnimatorSet mEndAnimatorSet = new AnimatorSet();
    private final AnimatorSet mPauseRecordAnimatorSet = new AnimatorSet();
    private final AnimatorSet mContinueRecordAnimatorSet = new AnimatorSet();
    private int centerX;
    private int centerY;

    //进度条
    private Paint mPartProgressPaint;
    private Paint mPartPointPaint;
    private int partProgressPaintColor = 0xFFEA7172;
    private int partPointColor = Color.WHITE;
    private float partRecordProgressWidth = 15;
    private final RectF progressRect = new RectF();
    private int maxRecordTime = 15 * 1000;
    private int minRecordTime = 2 * 1000;
    private int currentRecordTime;
    private List<PartRecordItem> recordItems;
    private final float defaultStartAngle = -90;
    //分段宽度（单位：度 取值0-360）
    private int partPointDegreeWidth = 2;

    private boolean isVisibleMinTimePoint = true;

    private int longClickMinTime = 500;
    private int animationTime = 400;

    private Action action = Action.recordVideo;
    private boolean isSupportPartRecord;//是否支持分段录制

    public enum Action{
        capture,//拍照
        recordVideo//录像
    }
    public RecordVideoButton(Context context) {
        this(context, null);
    }

    public RecordVideoButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordVideoButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecordVideoButton);
            mOutCircleColor = a.getColor(R.styleable.RecordVideoButton_out_circle_color, mOutCircleColor);
            innerRectColor = a.getColor(R.styleable.RecordVideoButton_inner_rect_color, innerRectColor);
            mInnerBackgroundCircleColor = a.getColor(R.styleable.RecordVideoButton_inner_background_circle_color, mInnerBackgroundCircleColor);
            mOutBackgroundCircleColor = a.getColor(R.styleable.RecordVideoButton_out_background_circle_color, mOutBackgroundCircleColor);
            partProgressPaintColor = a.getColor(R.styleable.RecordVideoButton_part_progress_color, partProgressPaintColor);
            partPointColor = a.getColor(R.styleable.RecordVideoButton_part_point_color, partPointColor);
            outCirclePaintWidth = a.getDimension(R.styleable.RecordVideoButton_out_circle_width, outCirclePaintWidth);
            innerRectMaxDividerWidth = a.getDimension(R.styleable.RecordVideoButton_inner_rect_max_divider_width, innerRectMaxDividerWidth);
            mMinCorner = a.getDimension(R.styleable.RecordVideoButton_inner_rect_min_corner, 0);
            maxRecordTime = a.getInt(R.styleable.RecordVideoButton_max_record_time, maxRecordTime);
            minRecordTime = a.getInt(R.styleable.RecordVideoButton_min_record_time, minRecordTime);
            partPointDegreeWidth = a.getInt(R.styleable.RecordVideoButton_part_point_degree_width, partPointDegreeWidth);
            partRecordProgressWidth = a.getDimension(R.styleable.RecordVideoButton_part_progress_width, partRecordProgressWidth);
            isVisibleMinTimePoint = a.getBoolean(R.styleable.RecordVideoButton_min_time_point_visible, isVisibleMinTimePoint);
            longClickMinTime = a.getInt(R.styleable.RecordVideoButton_long_click_min_time, longClickMinTime);
            animationTime = a.getInt(R.styleable.RecordVideoButton_state_change_animate_time, animationTime);
            isSupportPartRecord = a.getBoolean(R.styleable.RecordVideoButton_is_support_part_record, true);
            switch (a.getInt(R.styleable.RecordVideoButton_action,1)){
                case 0:
                    action = Action.capture;
                    break;
                case 1:
                    action = Action.recordVideo;
                    break;
            }
            a.recycle();
        }
        if (mMinCorner == 0){
            mMinCorner = dip2px(4);
        }
        init();
        mEndAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRecordState = RecordState.ORIGIN;
                invalidate();
            }
        });
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        //最外侧的圆环
        mOutCirclePaint = new Paint();
        initPaint(mOutCirclePaint,mOutCircleColor, outCirclePaintWidth, Paint.Style.STROKE, Paint.Cap.BUTT);
        //里面圆角矩形
        mInnerRectPaint = new Paint();
        initPaint(mInnerRectPaint,innerRectColor, 0, Paint.Style.FILL, Paint.Cap.BUTT);
        //圆角矩形外的圆
        mInnerBackgroundCirclePaint = new Paint();
        initPaint(mInnerBackgroundCirclePaint,mInnerBackgroundCircleColor, 0, Paint.Style.FILL, Paint.Cap.BUTT);
        //最外面的背景圆
        mOutBackgroundCirclePaint = new Paint();
        initPaint(mOutBackgroundCirclePaint,mOutBackgroundCircleColor, 0, Paint.Style.FILL, Paint.Cap.BUTT);
        //分段进度条
        mPartProgressPaint = new Paint();
        initPaint(mPartProgressPaint,partProgressPaintColor, partRecordProgressWidth, Paint.Style.STROKE, Paint.Cap.ROUND);
        recordItems = new ArrayList<>();
        //分段点画笔
        mPartPointPaint = new Paint();
        initPaint(mPartPointPaint,partPointColor, partRecordProgressWidth, Paint.Style.STROKE, Paint.Cap.BUTT);
    }

    private void initPaint(Paint paint,int paintColor, float paintWidth, Paint.Style style, Paint.Cap cap) {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(paintColor);
        if (paintWidth != 0) {
            paint.setStrokeWidth(paintWidth);
        }
        paint.setStyle(style);
        paint.setStrokeCap(cap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        centerX = width / 2;
        centerY = height / 2;

        mOutCircleWidth = (float) width * 2 / 3;
        mOutCircleRadius = mOutCircleWidth / 2;

        mMaxInnerBackgroundRadius = mOutCircleRadius;
        mMInInnerBackgroundRadius = mOutCircleRadius * 3 / 4;
        if (mInnerBackgroundRadius == 0) {
            mInnerBackgroundRadius = mMaxInnerBackgroundRadius;
        }

        mMaxOutBackgroundCircleRadius = (float) width / 2;
        mMinOutBackgroundCircleRadius = mOutCircleRadius;
        mMiddleOutBackgroundCircleRadius = (mMaxOutBackgroundCircleRadius + mMinOutBackgroundCircleRadius) / 2;
        if (mOutBackgroundRadius == 0) {
            mOutBackgroundRadius = mMinOutBackgroundCircleRadius;
        }

        if (mRecordState == RecordState.RECORDING || mRecordState == RecordState.PAUSE || mRecordState == RecordState.STOP) {
            //外侧背景圆
            drawOutBackgroundCircle(canvas);
            //内部圆角矩形外背景圆
            drawInnerBackgroundCircle(canvas);
        }
        if (mRecordState == RecordState.ORIGIN) {
            //外侧圆环
            drawOutCircle(canvas);
        }
        //内部圆角矩形
        drawInnerRoundRect(canvas);

        if (mRecordState == RecordState.RECORDING || mRecordState == RecordState.PAUSE || mRecordState == RecordState.STOP) {
            //录制进度条
            drawPartProgress(canvas);
        }
    }

    private void drawPartProgress(Canvas canvas) {

        float progressRadius = mOutBackgroundRadius - (float) partRecordProgressWidth / 2;
        progressRect.left = centerX - progressRadius;
        progressRect.right = centerX + progressRadius;
        progressRect.top = centerY - progressRadius;
        progressRect.bottom = centerY + progressRadius;

        //进度条
        float sweepAngle;
        float startAngle;
        startAngle = defaultStartAngle + getSweepAngle(getLastEndRecordTime());
        sweepAngle = getSweepAngle((currentRecordTime - getLastEndRecordTime()));
        canvas.drawArc(progressRect, startAngle, sweepAngle, false, mPartProgressPaint);

        for (int i = 0; i < recordItems.size(); i++) {
            PartRecordItem recordItem = recordItems.get(i);
            //分段的圆弧
            canvas.drawArc(progressRect, recordItem.startAngle, recordItem.sweepAngle, false, mPartProgressPaint);
        }

        //最小时间点
        if (isVisibleMinTimePoint && currentRecordTime < minRecordTime) {
            canvas.drawArc(progressRect, defaultStartAngle + getSweepAngle(minRecordTime), partPointDegreeWidth, false, mPartPointPaint);
        }

        for (int i = 0; i < recordItems.size(); i++) {
            PartRecordItem recordItem = recordItems.get(i);
            float startPointAngle;
            if (recordItem.endRecordTime == maxRecordTime){
                startPointAngle = defaultStartAngle;
            }else{
                startPointAngle = recordItem.startAngle + recordItem.sweepAngle + getRoundLineAngle(partRecordProgressWidth,progressRadius);
            }
            //分段点
            canvas.drawArc(progressRect, startPointAngle, partPointDegreeWidth, false, mPartPointPaint);
        }
    }
    //画笔圆角的宽度占圆的角度
    private float getRoundLineAngle(float lineWidth,float radius){
        return (float) (lineWidth/2 * 360 / (2 * Math.PI * radius));
    }

    private float getSweepAngle(int sweepTime) {
        return ((float) sweepTime / maxRecordTime) * 360;
    }

    private int getLastEndRecordTime() {
        if (recordItems.size() == 0) {
            return 0;
        } else {
            return recordItems.get(recordItems.size() - 1).endRecordTime;
        }
    }

    private void drawInnerBackgroundCircle(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, mInnerBackgroundRadius, mInnerBackgroundCirclePaint);
    }

    private void drawOutBackgroundCircle(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, mOutBackgroundRadius, mOutBackgroundCirclePaint);
    }

    private void drawInnerRoundRect(Canvas canvas) {
        mMaxRectWidth = mOutCircleWidth - innerRectMaxDividerWidth * 2;
        mMinRectWidth = mOutCircleRadius / 2;
        if (rectWidth == 0) {
            rectWidth = mMaxRectWidth;
        }
        if (corner == 0) {
            corner = rectWidth / 2;
        }

        mMaxCorner = mMaxRectWidth / 2;

        mRectF.left = centerX - rectWidth / 2;
        mRectF.right = centerX + rectWidth / 2;
        mRectF.top = centerY - rectWidth / 2;
        mRectF.bottom = centerY + rectWidth / 2;
        mInnerRectPaint.setAlpha(alpha);
        canvas.drawRoundRect(mRectF, corner, corner, mInnerRectPaint);
    }

    private void drawOutCircle(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, mOutCircleRadius, mOutCirclePaint);
    }

    public void setOnCaptureClickListener(OnCaptureClickListener onCaptureClickListener) {
        this.onCaptureClickListener = onCaptureClickListener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (action == Action.recordVideo) {
                    mRecordMode = RecordMode.SINGLE_CLICK;
                    if (mRecordState == RecordState.ORIGIN) {
                        mHandler.postDelayed(mClickRunnable, longClickMinTime);
                        startRecord();
                    } else if (mRecordState == RecordState.PAUSE) {
                        mHandler.postDelayed(mClickRunnable, longClickMinTime);
                        continueRecord();
                    } else if (mRecordState == RecordState.RECORDING) {
                        if (isSupportPartRecord){
                            onRecordPause();
                        }else{
                            recordStop();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (action == Action.recordVideo) {
                    if (mRecordMode == RecordMode.LONG_CLICK && mRecordState != RecordState.STOP) {
                        mHandler.removeCallbacks(mClickRunnable);
                        onRecordPause();
                    }
                }else if (action == Action.capture){
                    if (onCaptureClickListener != null){
                        onCaptureClickListener.onClick(this);
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    //暂停录制
    public void recordStop() {
        recordPart();
        mRecordState = RecordState.STOP;
        startPauseRecordAnimation();
        if (mOnRecordStateChangedListener != null) {
            mOnRecordStateChangedListener.onRecordStop();
        }
    }
    //开始录制
    public void startRecord(){
        recordItems.clear();
        mRecordState = RecordState.RECORDING;
        startBeginAnimation();
        if (mOnRecordStateChangedListener != null){
            mOnRecordStateChangedListener.onRecordStart();
        }
    }
    //继续录制
    public void continueRecord(){
        mRecordState = RecordState.RECORDING;
        startContinueRecordAnimation();
        if (mOnRecordStateChangedListener != null) {
            mOnRecordStateChangedListener.onContinueRecord();
        }
    }

    //开始录制失败
    public void startRecordFailed(){
        startPauseRecordAnimation();
    }

    //暂停录制回调
    public void onRecordPause(){
        if (mOnRecordStateChangedListener != null) {
            mOnRecordStateChangedListener.onRecordPause();
        }
    }


    //暂停录制
    public void recordPause() {
        mRecordState = RecordState.PAUSE;
        startPauseRecordAnimation();
        recordPart();
    }

    //记录分段
    public void recordPart() {
        if (isSupportPartRecord){
            PartRecordItem item = new PartRecordItem();
            item.startAngle = defaultStartAngle + getSweepAngle(getLastEndRecordTime());
            item.endRecordTime = currentRecordTime;
            item.sweepAngle = getSweepAngle(item.endRecordTime - getLastEndRecordTime());
            recordItems.add(item);
        }
        invalidate();

    }

    private void startBeginAnimation() {
        ObjectAnimator cornerAnimator = ObjectAnimator.ofFloat(this, "corner",
                mMaxCorner, mMinCorner)
                .setDuration(animationTime);
        ObjectAnimator rectSizeAnimator = ObjectAnimator.ofFloat(this, "rectWidth",
                mMaxRectWidth, mMinRectWidth)
                .setDuration(animationTime);
        ObjectAnimator outBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mOutBackgroundRadius",
                mMinOutBackgroundCircleRadius, mMaxOutBackgroundCircleRadius)
                .setDuration(animationTime);
        ObjectAnimator innerBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mInnerBackgroundRadius",
                mMaxInnerBackgroundRadius, mMInInnerBackgroundRadius)
                .setDuration(animationTime);
        mBeginAnimatorSet.playTogether(cornerAnimator, rectSizeAnimator, outBackgroundRadiusAnimator, innerBackgroundRadiusAnimator);
        mBeginAnimatorSet.start();
    }

    private void startPauseRecordAnimation() {
        ObjectAnimator outBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mOutBackgroundRadius",
                mMaxOutBackgroundCircleRadius, mMiddleOutBackgroundCircleRadius)
                .setDuration(animationTime);
        ObjectAnimator innerRectAlpha = ObjectAnimator.ofInt(this, "alpha",
                maxAlpha, minAlpha)
                .setDuration(animationTime);
        mPauseRecordAnimatorSet.playTogether(outBackgroundRadiusAnimator, innerRectAlpha);
        mPauseRecordAnimatorSet.start();
    }

    private void startContinueRecordAnimation() {
        ObjectAnimator outBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mOutBackgroundRadius",
                mMiddleOutBackgroundCircleRadius, mMaxOutBackgroundCircleRadius)
                .setDuration(animationTime);
        ObjectAnimator innerRectAlpha = ObjectAnimator.ofInt(this, "alpha",
                minAlpha, maxAlpha)
                .setDuration(animationTime);
        mContinueRecordAnimatorSet.playTogether(outBackgroundRadiusAnimator, innerRectAlpha);
        mContinueRecordAnimatorSet.start();
    }

    private void startEndAnimation() {
        ObjectAnimator cornerAnimator = ObjectAnimator.ofFloat(this, "corner",
                mMinCorner, mMaxCorner)
                .setDuration(animationTime);
        ObjectAnimator rectSizeAnimator = ObjectAnimator.ofFloat(this, "rectWidth",
                mMinRectWidth, mMaxRectWidth)
                .setDuration(animationTime);
        ObjectAnimator innerRectAlpha = ObjectAnimator.ofInt(this, "alpha",
                minAlpha, maxAlpha)
                .setDuration(animationTime);
        ObjectAnimator outBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mOutBackgroundRadius",
                mMaxOutBackgroundCircleRadius, mMinOutBackgroundCircleRadius)
                .setDuration(animationTime);
        ObjectAnimator innerBackgroundRadiusAnimator = ObjectAnimator.ofFloat(this, "mInnerBackgroundRadius",
                mMaxInnerBackgroundRadius, mMInInnerBackgroundRadius)
                .setDuration(animationTime);

        mEndAnimatorSet.playTogether(cornerAnimator, innerRectAlpha,rectSizeAnimator, outBackgroundRadiusAnimator, innerBackgroundRadiusAnimator);
        mEndAnimatorSet.start();
    }

    public void setOnRecordStateChangedListener(OnRecordStateChangedListener listener) {
        this.mOnRecordStateChangedListener = listener;
    }

    public void setCorner(float corner) {
        this.corner = corner;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public void setRectWidth(float rectWidth) {
        this.rectWidth = rectWidth;
    }

    public void setMInnerBackgroundRadius(float mInnerBackgroundRadius) {
        this.mInnerBackgroundRadius = mInnerBackgroundRadius;
    }

    public void setMOutBackgroundRadius(float mOutBackgroundRadius) {
        this.mOutBackgroundRadius = mOutBackgroundRadius;
        invalidate();
    }

    public void setMaxRecordTime(int maxRecordTime) {
        this.maxRecordTime = maxRecordTime;
    }

    public void setMinRecordTime(int minRecordTime) {
        this.minRecordTime = minRecordTime;
    }

    public RecordState getRecordState() {
        return mRecordState;
    }

    public void setCurrentRecordTime(int currentRecordTime) {
        if (currentRecordTime >= maxRecordTime){
            if (mRecordState == RecordState.RECORDING){
                this.currentRecordTime = maxRecordTime;
                recordStop();
            }
        }else{
            this.currentRecordTime = currentRecordTime;
            invalidate();
        }
    }

    public void deleteLastPartRecord() {
        if (recordItems.size() > 0) {
            if (recordItems.size() == 1) {
                resetRecordState();
            } else {
                recordItems.remove(recordItems.get(recordItems.size() - 1));
                currentRecordTime = getLastEndRecordTime();
                mRecordState = RecordState.PAUSE;
                invalidate();
            }
        }
    }
    //重置录制状态为original
    public void resetRecordState(){
        mRecordState = RecordState.ORIGIN;
        currentRecordTime = 0;
        recordItems.clear();
        startEndAnimation();
    }

    static class PartRecordItem {
        public int endRecordTime;
        public float sweepAngle;
        public float startAngle;
    }

    class ClickRunnable implements Runnable {

        @Override
        public void run() {
            mRecordMode = RecordMode.LONG_CLICK;
        }
    }

    public enum RecordState {
        /**
         * 初始化状态
         */
        ORIGIN,
        /**
         * 停止录制
         */
        STOP,
        /**
         * 录制中
         */
        RECORDING,
        /**
         * 暂停录制
         */
        PAUSE,
    }

    private enum RecordMode {
        /**
         * 单击录制模式
         */
        SINGLE_CLICK,
        /**
         * 长按录制模式
         */
        LONG_CLICK,
    }

    public interface OnCaptureClickListener{
        void onClick(View view);
    }

    public interface OnRecordStateChangedListener {

        /**
         * 开始录制
         */
        void onRecordStart();

        /**
         * 暂停录制
         */
        void onRecordPause();

        /**
         * 继续录制
         */
        void onContinueRecord();

        /**
         * 结束录制
         */
        void onRecordStop();

        /**
         * 删除最后一段
         */
        void onDeleteLastPart(int partsSize, long duration);

    }

    private int dip2px(float paramFloat) {
        return (int)(0.5F + paramFloat * getContext().getResources().getDisplayMetrics().density);
    }

    public void setAction(Action action) {
        this.action = action;
        resetRecordState();
        invalidate();
    }
}
