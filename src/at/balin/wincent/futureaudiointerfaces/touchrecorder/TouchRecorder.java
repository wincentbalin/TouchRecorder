package at.balin.wincent.futureaudiointerfaces.touchrecorder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

//
// Log:
//
// * Drawing                                                             v
// * Touch                                                               v
// * Dump of motion event                                                v
// * Viewing of motion event buffer                                      v
// * Settings (human-readability, default: view yes, debug no, save no)  v
// * Drawing of events depending on size and pressure                    v
// * Transparent colors to ease the inspection of previous events        v
// * Drawing of lines                                                    v
// * Creating file save dialog                                           v
// * Writing of motion event buffer to a file                            v
// * Creating file open dialog                                           v
// * Loading of an image                                                 v
// * Wrap MotionEvent into a TouchRecorderEvent to note image loads      v
// * Convert About activity to Help activity                             v
// * Make radius of circle and amount of units in the arc to options


/**
 * Main class of the Touch Recorder app.
 * 
 * @author Wincent Balin
 */
public class TouchRecorder extends Activity
{
    private GraphicsView graphics;
    
    public static final int FILE_DIALOG_LOAD_IMAGE = 1;
    public static final int FILE_DIALOG_SAVE_LOG = 2;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Request full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Create graphics view and make it main view
        graphics = new GraphicsView(this);
        setContentView(graphics);

        // Show short help message
        Toast.makeText(TouchRecorder.this, R.string.intro_help, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            handleBackKey(event);
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            handleBackKey(event);
            return true;
        }
        
        return super.onKeyUp(keyCode, event);
    }
    
    private void handleBackKey(KeyEvent event)
    {
        if(event.getRepeatCount() == 0)
            Toast.makeText(TouchRecorder.this, R.string.exit_help, Toast.LENGTH_SHORT).show();
        else
            finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
        case R.id.view:
            Bundle logViewBundle = new Bundle();
            logViewBundle.putString("LogData", graphics.getLog());
            Intent logViewIntent = new Intent(this, LogView.class);
            logViewIntent.putExtras(logViewBundle);
            startActivity(logViewIntent);
            return true;
        case R.id.save:
            // Open file save dialog
            Intent fileSaveIntent = new Intent(this, FileDialog.class);
            startActivityForResult(fileSaveIntent, FILE_DIALOG_SAVE_LOG);
            return true;
        case R.id.clear:
            graphics.clear();
            return true;
        case R.id.loadimage:
            // Open file load dialog
            Intent fileLoadIntent = new Intent(this, FileDialog.class);
            startActivityForResult(fileLoadIntent, FILE_DIALOG_LOAD_IMAGE);
            return true;
        case R.id.preferences:
            startActivity(new Intent(this, Preferences.class));
            return true;
        case R.id.help:
            Intent helpIntent = new Intent(this, Help.class);
            startActivity(helpIntent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == FILE_DIALOG_LOAD_IMAGE)
            {
                graphics.loadImage(data.getStringExtra(FileDialog.FILENAME));
            }
            else if(requestCode == FILE_DIALOG_SAVE_LOG)
            {
                graphics.saveLog(data.getStringExtra(FileDialog.FILENAME));
            }
        }
    }
    
    /**
     * A canvas view.
     * 
     * @author Wincent Balin
     */
    class GraphicsView extends View
    {
        private Bitmap bitmap;
        private Canvas canvas;
        
        private Paint bitmapPaint;
        private Paint touchStartPaint;
        private Paint touchRestPaint;
        
        private List<Event> log = new ArrayList<Event>();
        private int lastEventIndex = 0;
        
        private static final int MAX_EVENTS = 256;
        private float[] previousX = new float[MAX_EVENTS];
        private float[] previousY = new float[MAX_EVENTS];
        private RectF[] previousBounds = new RectF[MAX_EVENTS];
        
        private boolean clearBackgroundFlag = true; // We clear the canvas on start
        private boolean drawImageFlag = false;
        private boolean drawEventFlag = false;

        private float radiusOf1 = 80.0f;
        private float maxPressure = 360.0f;
        
        private final int backgroundColor = Color.WHITE;
        private final int touchStartColor = Color.argb(200, 126, 0, 33); // Semi-transparent wine red
        private final int touchRestColor = Color.argb(200, 0, 0, 0); // Semi-transparent black
        
        private Bitmap backgroundBitmap = null;
        
        private final float STROKE_WIDTH_HAIRLINE = 1.0f;        
        private final float STROKE_WIDTH_FAT = 5.0f;

    
        public GraphicsView(Context context)
        {
            super(context);

            // Initialize painters
            bitmapPaint = new Paint();
            bitmapPaint.setAntiAlias(true);
            bitmapPaint.setStyle(Style.FILL_AND_STROKE);

            touchStartPaint = new Paint(); 
            touchStartPaint.setColor(touchStartColor);
            touchStartPaint.setAntiAlias(true);
            touchStartPaint.setStyle(Style.STROKE);
            
            touchRestPaint = new Paint();
            touchRestPaint.setColor(touchRestColor);
            touchRestPaint.setAntiAlias(true);
            touchRestPaint.setStyle(Style.STROKE);
            
            // Initialize array of previous boundaries
            Arrays.fill(previousBounds, new RectF(-100.0f, -100.0f, -100.0f, -100.0f));
            
            // Do not change orientation, as such change restarts the whole activity and wipes the log!
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh)
        {
            super.onSizeChanged(w, h, oldw, oldh);
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
        }
        
        @Override
        protected void onDraw(Canvas screenCanvas)
        {
            if(clearBackgroundFlag)
            {
                canvas.drawColor(backgroundColor);
                clearBackgroundFlag = false;
            }
            
            if(drawImageFlag)
            {
                canvas.drawBitmap(backgroundBitmap, 0, 0, bitmapPaint);
                drawImageFlag = false;
            }
            
            if(drawEventFlag)
            {
                for(int eventIndex = lastEventIndex; eventIndex < log.size(); eventIndex++)
                {
                    Event event = log.get(eventIndex);
                    
                    if(!event.isMotionEvent()) // Image load event
                        continue;
                    
                    MotionEvent motionEvent = event.getMotionEvent();
                    final int motionEventAction = motionEvent.getAction() & MotionEvent.ACTION_MASK;

                    switch(motionEventAction)
                    {
                    case MotionEvent.ACTION_DOWN:
                        visualizeActionDown(motionEvent);
                        break;
                        
                    case MotionEvent.ACTION_MOVE:
                        visualizeActionMove(motionEvent);
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        visualizeActionUp(motionEvent);
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        visualizeActionPointerDown(motionEvent);
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        visualizeActionPointerUp(motionEvent);
                        break;
                    }
                    
                }
                
                lastEventIndex = log.size();
                drawEventFlag = false;
            }
            
            // Paint buffer
            screenCanvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        }
        
        private void visualizeActionDown(MotionEvent event)
        {
            for(int i = 0; i < event.getPointerCount(); i++)
            {
                final int pointerId = event.getPointerId(i);

                final float x = event.getX(i);
                final float y = event.getY(i);
                final float size = event.getSize(i);
                final float pressure = event.getPressure(i);
                
                drawEvent(canvas, x, y, size, pressure, pointerId, false, touchStartPaint);
            }
        }
        
        private void visualizeActionMove(MotionEvent event)
        {
            for(int i = 0; i < event.getPointerCount(); i++)
            {
                final int pointerId = event.getPointerId(i);
                
                final int historySize = event.getHistorySize();
                
                for(int h = 0; h < historySize; h++)
                {
                    final float x = event.getHistoricalX(i, h);
                    final float y = event.getHistoricalY(i, h);
                    
                    drawTransition(canvas, x, y, pointerId, touchRestPaint);
                    
                    final float size = event.getHistoricalSize(i, h);
                    final float pressure = event.getHistoricalPressure(i, h);
                    
                    drawEvent(canvas, x, y, size, pressure, pointerId, false, touchRestPaint);
                }

                final float x = event.getX(i);
                final float y = event.getY(i);
                
                drawTransition(canvas, x, y, pointerId, touchRestPaint);
                
                final float size = event.getSize(i);
                final float pressure = event.getPressure(i);
                
                drawEvent(canvas, x, y, size, pressure, pointerId, false, touchRestPaint);
            }
        }
        
        private void visualizeActionUp(MotionEvent event)
        {
            for(int i = 0; i < event.getPointerCount(); i++)
            {
                final int pointerId = event.getPointerId(i);

                final float x = event.getX(i);
                final float y = event.getY(i);
                
                drawTransition(canvas, x, y, pointerId, touchRestPaint);
                
                final float size = event.getSize(i);
                final float pressure = event.getPressure(i);
                
                drawEvent(canvas, x, y, size, pressure, pointerId, true, touchRestPaint);
            }
        }
        
        private void visualizeActionPointerDown(MotionEvent event)
        {
            final int pointerIndex = event.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            final int pointerId = event.getPointerId(pointerIndex);

            final float x = event.getX(pointerIndex);
            final float y = event.getY(pointerIndex);
            final float size = event.getSize(pointerIndex);
            final float pressure = event.getPressure(pointerIndex);
            
            drawEvent(canvas, x, y, size, pressure, pointerId, false, touchStartPaint);
        }
        
        private void visualizeActionPointerUp(MotionEvent event)
        {
            final int pointerIndex = event.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            final int pointerId = event.getPointerId(pointerIndex);

            final float x = event.getX(pointerIndex);
            final float y = event.getY(pointerIndex);
            
            drawTransition(canvas, x, y, pointerId, touchRestPaint);
            
            final float size = event.getSize(pointerIndex);
            final float pressure = event.getPressure(pointerIndex);
            
            drawEvent(canvas, x, y, size, pressure, pointerId, true, touchRestPaint);
        }
        
        private void drawTransition(Canvas canvas, float x, float y, int pointerId, Paint paint)
        {
            paint.setStrokeWidth(STROKE_WIDTH_HAIRLINE);
            canvas.drawLine(previousX[pointerId], previousY[pointerId], x, y, paint);
        }
        
        private void drawEvent(Canvas canvas, float x, float y, float size, float pressure, int pointerId, boolean overlap, Paint paint)
        {
            final float radius = Math.max(1.0f, size * radiusOf1);
            
            final float left = x - radius;
            final float top = y - radius;
            final float right = x + radius;
            final float bottom = y + radius;
            RectF bounds = new RectF(left, top, right, bottom);
            
            if(overlap || (!overlap && !RectF.intersects(bounds, previousBounds[pointerId])))
            {
                // Express one thousandth of one pressure unit as one degree angle
                final float pressureAngle = Math.min(pressure, maxPressure) * (360.0f / maxPressure) * 1000.0f;

                paint.setStrokeWidth(STROKE_WIDTH_FAT);
                canvas.drawArc(bounds, 0.0f, pressureAngle, false, paint);

                if(pressureAngle < 360.0f)
                {
                    paint.setStrokeWidth(STROKE_WIDTH_HAIRLINE);
                    canvas.drawArc(bounds, pressureAngle, 360.0f, false, paint);
                }
                
                // Store boundaries
                previousBounds[pointerId] = bounds;
            }
            
            // Store coordinates
            previousX[pointerId] = x;
            previousY[pointerId] = y;
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            // Dump motion event to debug log
            final boolean debugIsHumanReadable = Preferences.debugFormatIsHumanReadable(getContext());
            Log.i("event", describeEvent(event, debugIsHumanReadable));
            
            // Store event
            log.add(new Event(MotionEvent.obtain(event)));
            
            // Beginning a touch, update dimensional settings
            if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
                updateDimensionalSettings();

            // Notify drawing method about new event
            drawEventFlag = true;
            invalidate();
            
            return true;
        }
        
        private void updateDimensionalSettings()
        {
            final Context context = getContext();

            final String sizeOf1String = Preferences.radiusOfOne(context);
            final String maxPressureString = Preferences.pressureOfOne(context);
            
            final float displayDensity = context.getResources().getDisplayMetrics().density;

            try
            {
                radiusOf1 = Float.parseFloat(sizeOf1String);
                radiusOf1 *= displayDensity;
            }
            catch(NumberFormatException e)
            {
                Toast.makeText(context, R.string.wrong_numeric_preference, Toast.LENGTH_LONG).show();
            }

            try
            {
                maxPressure = Float.parseFloat(maxPressureString);
            }
            catch(NumberFormatException e)
            {
                Toast.makeText(context, R.string.wrong_numeric_preference, Toast.LENGTH_LONG).show();
            }
        }
        
        /**
         * Clear both canvas and log list.
         */
        public void clear()
        {
            log.clear();
            lastEventIndex = 0;
            clearBackgroundFlag = true;
            invalidate();
        }
        
        /**
         * Load image onto canvas.
         * 
         * @param fileName Name of the image file
         */
        public void loadImage(String fileName)
        {
            // Load and decode file
            backgroundBitmap = BitmapFactory.decodeFile(fileName);
            
            // Store this event
            log.add(new Event(fileName, SystemClock.uptimeMillis()));
            
            // Make this update known to the system
            drawImageFlag = true;
            invalidate();
        }
        
        /**
         * Save log list into a file.
         * 
         * @param fileName Name of the log file
         */
        public void saveLog(String fileName)
        {
            FileWriter writer;
            
            try
            {
                writer = new FileWriter(fileName);
                
                boolean saveDataFormatIsHumanReadable = Preferences.saveFormatIsHumanReadable(getContext());
                
                writer.write(createLogData(saveDataFormatIsHumanReadable));
                
                writer.close();
            }
            catch (IOException e)
            {
                Toast.makeText(TouchRecorder.this, R.string.something_wrong_with_file, Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        private String getLog()
        {
            boolean logViewFormatIsHumanReadable = Preferences.viewFormatIsHumanReadable(getContext());
            
            return createLogData(logViewFormatIsHumanReadable);
        }
        
        private String createLogData(boolean humanReadable)
        {
            StringBuilder buffer = new StringBuilder();
            
            for(int i = 0; i < log.size(); i++)
                buffer.append(describeEvent(log.get(i), humanReadable));
            
            return buffer.toString();
        }
        
        private String describeEvent(Event event, boolean humanReadable)
        {
            if(event.isMotionEvent())
            {
                return describeEvent(event.getMotionEvent(), humanReadable);
            }
            else
            {
                final String prefix = "        ";
                StringBuilder buffer = new StringBuilder();
                
                // Create prefix
                buffer.append("Image ");
                
                // Append image file name
                buffer.append(event.getImageFileName());
                
                if(humanReadable)
                {
                    buffer.append('\n');
                    buffer.append(prefix);
                }
                else
                {
                    buffer.append(' ');
                }
                
                // Append time
                buffer.append("At ");
                buffer.append(event.getImageLoadTime());
                
                if(humanReadable)
                    buffer.append(" ms");
                
                buffer.append('\n');
                
                return buffer.toString();
            }
        }

        private final String[] actionNames = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP"};

        private String describeEvent(MotionEvent event, boolean humanReadable)
        {
            StringBuilder buffer = new StringBuilder();

            // Create prefix; describe event and time
            buffer.append("Event ");
            
            // Describe action
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            
            if(humanReadable)
                buffer.append("with action ");
            
            buffer.append(action < actionNames.length ? actionNames[action] : "UNKNOWN");
            buffer.append(':');
            
            buffer.append(humanReadable ? '\n' : ' ');
            
            final String prefix = "        ";
            
            // If this is a DOWN action, look whether it was started at the edge
            final int edgeFlags = event.getEdgeFlags();
            
            if(action == MotionEvent.ACTION_DOWN && edgeFlags != 0)
            {
                if(humanReadable)
                {
                    buffer.append(prefix);
                    buffer.append("Following screen edges had been crossed: ");
                }
                else
                {
                    buffer.append("edges ");
                }
                
                if((edgeFlags & MotionEvent.EDGE_BOTTOM) != 0)
                {
                    buffer.append("BOTTOM");
                    buffer.append(humanReadable ? ' ' : '/');
                }

                if((edgeFlags & MotionEvent.EDGE_LEFT) != 0)
                {
                    buffer.append("LEFT");
                    buffer.append(humanReadable ? ' ' : '/');
                }

                if((edgeFlags & MotionEvent.EDGE_RIGHT) != 0)
                {
                    buffer.append("RIGHT");
                    buffer.append(humanReadable ? ' ' : '/');
                }

                if((edgeFlags & MotionEvent.EDGE_TOP) != 0)
                {
                    buffer.append("TOP");
                    buffer.append(humanReadable ? ' ' : '/');
                }
                
                buffer.append(humanReadable ? '\n' : ' ');
            }
            
            // If this is a POINTER_DOWN or POINTER_UP action, describe the source pointer
            if(action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            {
                if(humanReadable)
                {
                    buffer.append(prefix);
                    
                    buffer.append("Created by pointer ");
                }
                else
                {
                    buffer.append("pointer ");
                }
                
                
                buffer.append(event.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT);
                
                buffer.append(humanReadable ? '\n' : ' ');
            }
            
            // If we moved, print history
            if(action == MotionEvent.ACTION_MOVE)
            {
                if(!humanReadable)
                {
                    buffer.append("history ");
                    buffer.append(event.getHistorySize());
                    buffer.append(' ');
                    
                    if(event.getHistorySize() > 0)
                    {
                        buffer.append("pointers ");
                        buffer.append(event.getPointerCount());
                        buffer.append(' ');
                    }
                }
                
                for(int h = 0; h < event.getHistorySize(); h++)
                {
                    for(int i = 0; i < event.getPointerCount(); i++)
                    {
                        if(humanReadable)
                            buffer.append(prefix);
                        
                        buffer.append("At ");
                        buffer.append(event.getHistoricalEventTime(h));
                        buffer.append(humanReadable ? " ms " : " ");
                        
                        buffer.append("pointer ");
                        buffer.append(i);
                        buffer.append(" known as ");
                        buffer.append(event.getPointerId(i));
                        buffer.append(": ");
                        
                        buffer.append(humanReadable ? "x = " : "x ");
                        buffer.append(event.getHistoricalX(i, h));
                        buffer.append(humanReadable ? "  " : " ");
                        
                        buffer.append(humanReadable ? "y = " : "y ");
                        buffer.append(event.getHistoricalY(i, h));
                        buffer.append(humanReadable ? "  " : " ");
                        
                        buffer.append(humanReadable ? "size = " : "size ");
                        buffer.append(event.getHistoricalSize(i, h));
                        buffer.append(humanReadable ? "  " : " ");
                        
                        buffer.append(humanReadable ? "pressure = " : "pressure ");
                        buffer.append(event.getHistoricalPressure(i, h));
                        buffer.append(humanReadable ? "  " : " ");
                        
                        buffer.append(humanReadable ? '\n' : ' ');
                    }
                }
            }
            
            // Describe every pointer for the current time
            if(!humanReadable)
            {
                buffer.append("pointers ");
                buffer.append(event.getPointerCount());
                buffer.append(' ');
            }

            
            for(int i = 0; i < event.getPointerCount(); i++)
            {
                if(humanReadable)
                    buffer.append(prefix);
                
                buffer.append("At ");
                buffer.append(event.getEventTime());
                buffer.append(humanReadable ? " ms " : " ");

                buffer.append("pointer ");
                buffer.append(i);
                buffer.append(" known as ");
                buffer.append(event.getPointerId(i));
                buffer.append(": ");
                
                buffer.append(humanReadable ? "x = " : "x ");
                buffer.append(event.getX(i));
                buffer.append(humanReadable ? "  " : " ");
                
                buffer.append(humanReadable ? "y = " : "y ");
                buffer.append(event.getY(i));
                buffer.append(humanReadable ? "  " : " ");
                
                buffer.append(humanReadable ? "size = " : "size ");
                buffer.append(event.getSize(i));
                buffer.append(humanReadable ? "  " : " ");
                
                buffer.append(humanReadable ? "pressure = " : "pressure ");
                buffer.append(event.getPressure(i));
                buffer.append(humanReadable ? "  " : " ");
                
                buffer.append(humanReadable ? '\n' : ' ');
            }
            
            buffer.append('\n');
            
            
            // Return result
            return buffer.toString();
        }
    }
    
    /**
     * Container of either a motion event, when an input has been made, or of an image load event, when an image has been loaded.
     * 
     * @author Wincent Balin
     */
    class Event
    {
        private MotionEvent motionEvent = null;
        private String imageFileName;
        private long imageLoadTime;
        
        /**
         * Create TouchRecorder event out of a MotionEvent.
         * 
         * @param motionEvent Event to store
         */
        public Event(MotionEvent motionEvent)
        {
            this.motionEvent = motionEvent;
        }
        
        /**
         * Create description of image loading event.
         * 
         * @param imageFileName Image file loaded
         * @param imageLoadTime Time the image was loaded at
         */
        public Event(String imageFileName, long imageLoadTime)
        {
            this.imageFileName = imageFileName;
            this.imageLoadTime = imageLoadTime;
        }
        
        /**
         * Answer whether we store a motion event.
         * 
         * @return True when motion event has been stored here, else false
         */
        public boolean isMotionEvent()
        {
            return (motionEvent != null);
        }
        
        /**
         * Return motion event.
         * 
         * @return Motion event
         */
        public MotionEvent getMotionEvent()
        {
            return motionEvent;
        }
        
        /**
         * Give the name of the image file loaded.
         * 
         * @return Image file name
         */
        public String getImageFileName()
        {
            return imageFileName;
        }
        
        /**
         * Give the time the image was loaded at.
         *
         * @return Time in milliseconds since the last start of the device.
         */
        public long getImageLoadTime()
        {
            return imageLoadTime;
        }
    }
}
