package at.balin.wincent.futureaudiointerfaces.touchrecorder;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences screen.
 * 
 * @author Wincent Balin
 */
public class Preferences extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
    /**
     * Option whether the data format in the view screen is human-readable.
     * 
     * @param context Application context
     * @return Boolean option
     */
    public static boolean viewFormatIsHumanReadable(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("view", true);
    }
    
    /**
     * Option whether the data format in the saved files is human-readable.
     * 
     * @param context Application context
     * @return Boolean option
     */
    public static boolean saveFormatIsHumanReadable(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("save", false);
    }
    
    /**
     * Option whether the data format in the debug messages is human-readable.
     * 
     * @param context Application context
     * @return Boolean option
     */
    public static boolean debugFormatIsHumanReadable(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("debug", false);
    }
    
    /**
     * Option of radius of circle which equals event size of 1.
     * 
     * @param context Application context
     * @return Float option as string
     */
    public static String radiusOfOne(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("size", "80.0");
    }
    
    /**
     * Option of how many 1/1000th of a pressure unit the event circle may contain at most.
     * 
     * @param context Application context
     * @return Float option as string
     */
    public static String pressureOfOne(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("pressure", "360.0");
    }
}
