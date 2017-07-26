package ve.com.abicelis.prizewheellib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by abicelis on 25/7/2017.
 */

public class PrizeWheelView extends AppCompatImageView {

    private static Bitmap imageOriginal, imageScaled;
    private static Matrix matrix;
    public GestureDetector gestureDetector;
    public WheelTouchListener touchListener;

    // needed for detecting inverted rotations
    public boolean[] quadrantTouched = new boolean[] { false, false, false, false, false };
    private boolean allowRotating = true;



    private int wheelHeight, wheelWidth;

    public PrizeWheelView(Context context) {
        super(context);
        init(context, null);
    }

    public PrizeWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    public PrizeWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {
        this.setBackgroundColor(Color.RED);
        setScaleType(ScaleType.MATRIX);

        // load the image only once
        if (imageOriginal == null) {
            imageOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.default_wheel);
        }

        // initialize the matrix only once
        if (matrix == null) {
            matrix = new Matrix();
        } else {
            // not needed, you can also post the matrix immediately to restore the old state
            matrix.reset();
        }

        gestureDetector = new GestureDetector(context, new WheelGestureListener(this));
        touchListener = new WheelTouchListener();
        setOnTouchListener(touchListener);



        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // method called more than once, but the values only need to be initialized one time
                if (wheelHeight == 0 || wheelWidth == 0) {
                    wheelHeight = getHeight();
                    wheelWidth = getWidth();

                    // resize
                    Matrix resize = new Matrix();
                    resize.postScale((float)Math.min(wheelWidth, wheelHeight) / (float)imageOriginal.getWidth(), (float)Math.min(wheelWidth, wheelHeight) / (float)imageOriginal.getHeight());
                    imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                    // translate to the image view's center
                    float translateX = wheelWidth / 2 - imageScaled.getWidth() / 2;
                    float translateY = wheelHeight / 2 - imageScaled.getHeight() / 2;
                    matrix.postTranslate(translateX, translateY);

                    setImageBitmap(imageScaled);
                    setImageMatrix(matrix);

                    touchListener.setDimensions(wheelWidth, wheelHeight);






                    Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    whitePaint.setColor(Color.WHITE);
                    whitePaint.setStyle(Paint.Style.STROKE);
                    whitePaint.setStrokeWidth(1f);
                    whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);

                    Bitmap mask = Bitmap.createBitmap(wheelWidth, wheelHeight, Bitmap.Config.ARGB_8888);
                    Bitmap result = Bitmap.createBitmap(wheelWidth, wheelHeight, Bitmap.Config.ARGB_8888);
                    Canvas resultCanvas = new Canvas(result);
                    Canvas maskCanvas = new Canvas(mask);


                    @DrawableRes int[] images = new int[]{R.drawable.burger, R.drawable.chicken, R.drawable.hummus, R.drawable.noodles, R.drawable.pasta};
                    float startAngle = 0f;
                    float sweepAngle = (360.0f / images.length);
                    RectF box = new RectF(2, 2, wheelWidth-2 , wheelHeight-2);


                    for(int i=3; i<images.length; i++) {
                        maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        maskCanvas.drawArc(box, startAngle, sweepAngle, true, whitePaint);

                        Bitmap temp = Bitmap.createBitmap(wheelWidth, wheelHeight, Bitmap.Config.ARGB_8888);
                        Canvas tempCanvas = new Canvas(temp);

                        Bitmap original = BitmapFactory.decodeResource(getContext().getResources(), images[i]);
                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                        tempCanvas.drawBitmap(original, 0, 0, null);
                        tempCanvas.drawBitmap(mask, 0, 0, paint);
                        paint.setXfermode(null);

                        //Draw result after performing masking
                        resultCanvas.drawBitmap(result, 0, 0, new Paint());

                        startAngle += sweepAngle;
                    }




                    //setImageBitmap(mask);
                    setImageBitmap(result);



                }
            }
        });

    }



    /**
     * Rotate the wheel.
     *
     * @param degrees The degrees, the wheel should get rotated.
     */
    public void rotateWheel(float degrees) {
        matrix.postRotate(degrees, wheelWidth / 2, wheelHeight / 2);
        setImageMatrix(matrix);
    }


    /**
     * Reset touch quadrants to false.
     *
     */
    public void resetQuadrants() {
        for (int i = 0; i < quadrantTouched.length; i++) {
            quadrantTouched[i] = false;
        }
    }

    /**
     * @return The selected quadrant.
     */
    public static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }








    private class WheelTouchListener implements View.OnTouchListener {

        private double startAngle;
        private double wheelWidth;
        private double wheelHeight;


        private void setDimensions(double wheelWidth, double wheelHeight) {
            this.wheelWidth = wheelWidth;
            this.wheelHeight = wheelHeight;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    resetQuadrants();
                    startAngle = getAngle(event.getX(), event.getY());
                    allowRotating = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    double currentAngle = getAngle(event.getX(), event.getY());
                    rotateWheel((float) (startAngle - currentAngle));
                    startAngle = currentAngle;
                    break;

                case MotionEvent.ACTION_UP:
                    allowRotating = true;
                    break;
            }

            // set the touched quadrant to true
            quadrantTouched[getQuadrant(event.getX() - (wheelWidth / 2), wheelHeight - event.getY() - (wheelHeight / 2))] = true;

            //Notify gesture detector
            gestureDetector.onTouchEvent(event);

            return true;
        }

        /**
         * @return The angle of the unit circle with the image view's center
         */
        private double getAngle(double xTouch, double yTouch) {
            double x = xTouch - (wheelWidth / 2d);
            double y = wheelHeight - yTouch - (wheelHeight / 2d);

            return (Math.atan2(y,x) * 180) / Math.PI;
        }

    }




    public class WheelGestureListener implements GestureDetector.OnGestureListener {


        private static final float INITIAL_FLING_VELOCITY_DAMPENING = 3;     //Number between 1 (no dampening) and 5 (Lots of dampening). Default 3
        private static final float FLING_VELOCITY_DAMPENING = 1.025F;     //Number between 1 (no dampening) and 1.1 (Lots of dampening). Default 1.06
        private PrizeWheelView wheel;



        public WheelGestureListener(PrizeWheelView wheel) {
            this.wheel = wheel;

        }


        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            // get the quadrant of the start and the end of the fling
            int q1 = getQuadrant(e1.getX() - (wheelWidth / 2), wheelHeight - e1.getY() - (wheelHeight / 2));
            int q2 = getQuadrant(e2.getX() - (wheelWidth / 2), wheelHeight - e2.getY() - (wheelHeight / 2));

            // the inversed rotations
            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY))
                    || (q1 == 3 && q2 == 3)
                    || (q1 == 1 && q2 == 3)
                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY))
                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
                    || (q1 == 2 && q2 == 4 && quadrantTouched[3])
                    || (q1 == 4 && q2 == 2 && quadrantTouched[3])) {

                post(new FlingRunnable( (-1 * (velocityX + velocityY)) / INITIAL_FLING_VELOCITY_DAMPENING ));
            } else {
                // the normal rotation
                post(new FlingRunnable( (velocityX + velocityY) / INITIAL_FLING_VELOCITY_DAMPENING ));
            }

            return true;
        }


        /**
         * A {@link Runnable} for animating the the dialer's fling.
         */
        private class FlingRunnable implements Runnable {

            private float velocity;

            public FlingRunnable(float velocity) {
                this.velocity = velocity;
            }

            @Override
            public void run() {
                if (Math.abs(velocity) > 5 && allowRotating) {
                    wheel.rotateWheel(velocity / 75);
                    velocity /= FLING_VELOCITY_DAMPENING;

                    // post this instance again
                    wheel.post(this);
                }
            }
        }
    }

}