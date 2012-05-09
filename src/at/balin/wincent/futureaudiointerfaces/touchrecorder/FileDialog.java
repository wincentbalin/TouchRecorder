package at.balin.wincent.futureaudiointerfaces.touchrecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * File dialog implementation for Android.
 * 
 * @author Wincent Balin
 */
public class FileDialog extends Activity implements OnItemClickListener, OnClickListener, OnKeyListener
{
    /**
     * Map key of the resulting file name.
     */
    public static final String FILENAME = "FILENAME";

    protected final String initialPath = "/sdcard/";

    protected String currentPath = initialPath;

    protected Stack<String> pathStack;
    protected ArrayList<String> pathFileNames;

    private ArrayList<String> directoryNames;
    private ArrayList<String> regularFileNames;

    private TextView filePath;
    private ListView fileNames;
    private EditText fileName;
    private Button fileNameAccept;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filedialog);

        pathStack = new Stack<String>();

        filePath = (TextView) findViewById(R.id.filepath);
        fileNames = (ListView) findViewById(R.id.filenames);
        fileName = (EditText) findViewById(R.id.filename);
        fileNameAccept = (Button) findViewById(R.id.filename_accept);

        fileNames.setOnItemClickListener(this);
        fileName.setOnKeyListener(this);
        fileNameAccept.setOnClickListener(this);

        listPath(initialPath);
    }

    /**
     * List path in the dialog.
     * 
     * @param path
     *            Path to list
     */
    private void listPath(String path)
    {
        directoryNames = new ArrayList<String>();
        regularFileNames = new ArrayList<String>();

        File pathFile = new File(path);
        File[] pathFiles = pathFile.listFiles();

        // In case there are no contents, do not bother
        if(pathFiles == null)
        {
            Toast.makeText(FileDialog.this, R.string.something_wrong_with_directory, Toast.LENGTH_LONG).show();
            return;
        }

        // Sort contents into directories and files
        for(int i = 0; i < pathFiles.length; i++)
        {
            File file = pathFiles[i];

            if (file.isDirectory())
                directoryNames.add(file.getName());
            else
                regularFileNames.add(file.getName());
        }

        // Sort resulting file name lists
        Collections.sort(directoryNames);
        Collections.sort(regularFileNames);

        // Append regular files after directories
        pathFileNames = new ArrayList<String>();
        for (String directoryName : directoryNames)
            pathFileNames.add(directoryName + File.separatorChar);
        pathFileNames.addAll(regularFileNames);

        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this,
                R.layout.filedialogentry, pathFileNames);
        fileNames.setAdapter(fileList);

        currentPath = path;
        filePath.setText(currentPath);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
            long id)
    {
        String fileBaseName = pathFileNames.get(position);
        String fileAbsolutePath = currentPath + fileBaseName;
        File file = new File(fileAbsolutePath);

        if (file.isDirectory())
        {
            if (file.canRead())
            {
                pathStack.push(currentPath);
                listPath(fileAbsolutePath);
            } else
            {
                Toast.makeText(FileDialog.this, R.string.cannot_read_directory,
                        Toast.LENGTH_SHORT).show();
            }
        } else
        {
            String currentFileNameContents = fileName.getText().toString();

            if (currentFileNameContents.equals(fileBaseName))
            {
                returnWithResult(fileAbsolutePath);
            } else
            {
                fileName.setText(fileBaseName);
                Toast.makeText(FileDialog.this,
                        R.string.accept_filename_advice, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        if (v == fileNameAccept)
        {
            String currentFileNameContents = fileName.getText().toString();

            if (currentFileNameContents.length() == 0)
                Toast.makeText(FileDialog.this, R.string.enter_filename_first,
                        Toast.LENGTH_SHORT).show();
            else
                returnWithResult(currentPath + currentFileNameContents);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP)
        {
            if (pathStack.isEmpty())
            {
                returnWithoutResult();
            } else
            {
                String previousPath = pathStack.pop();
                listPath(previousPath);
            }

            return true;
        }

        if (v == fileName)
        {
            if (keyCode == KeyEvent.KEYCODE_ENTER)
            {
                onClick(fileNameAccept);

                return true;
            }
        }

        return false;
    }

    /**
     * Close file dialog with result.
     * 
     * @param resultingFileName
     *            Resulting file name
     */
    private void returnWithResult(String resultingFileName)
    {
        Intent intent = getIntent();
        intent.putExtra(FILENAME, resultingFileName);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Close file dialog without result.
     */
    private void returnWithoutResult()
    {
        setResult(RESULT_CANCELED, getIntent());
        finish();
    }
}
