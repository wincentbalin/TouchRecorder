package at.balin.wincent.futureaudiointerfaces.touchrecorder;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LogView extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logview);
        
        String data = getIntent().getExtras().getString("LogData");
        
        if(data.length() > 0)
        {
            TextView logContent = (TextView) findViewById(R.id.log_content);
            logContent.setText(data);
        }
    }
}
