/*
 * Copyright (c) 2017. David de Andrés and Juan Carlos Ruiz, DISCA - UPV, Development of apps for mobile devices.
 */

package labs.sdm.l0403_files;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    // Hold references to View
    Spinner spinner;
    EditText etFileContent;
    Button bSave;

    // Constants identifying the current operation
    private static final int READ_RESOURCES = 0;
    private static final int READ_INTERNAL_STORAGE = 1;
    private static final int READ_EXTERNAL_STORAGE = 2;
    private static final int READ_PUBLIC_EXTERNAL_STORAGE = 3;
    private static final int WRITE_INTERNAL_STORAGE = 4;
    private static final int WRITE_EXTERNAL_STORAGE = 5;
    private static final int WRITE_PUBLIC_EXTERNAL_STORAGE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get references to the View
        etFileContent = findViewById(R.id.etFileContent);
        bSave = findViewById(R.id.bSave);
        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Resources cannot be overwritten,
                // so the Save button is disabled when Resources are selected
                if (position == 0) {
                    bSave.setEnabled(false);
                } else {
                    bSave.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /*
        Performs the required checks to read the file according to the selected destination
    */
    public void saveFile(View view) {

        // Determine the destination file to load according to the selected item
        switch (spinner.getSelectedItemPosition()) {

            // Application internal storage
            case 1:
                writeFile(WRITE_INTERNAL_STORAGE);
                break;

            // Application external storage
            case 2:
                // Check the external memory is writable
                // and that the user has granted permission for writing it (if API < 19)
                if (isExternalmemoryWritable()) {
                    if ((Build.VERSION.SDK_INT > 18) ||
                            (checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))) {
                        writeFile(WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(this, R.string.external_memory_not_mounted, Toast.LENGTH_SHORT).show();
                }
                break;

            // Public external storage (DOWNLOAD)
            case 3:
                // Check the external memory is writable and that the user has granted permission for writing it
                if (isExternalmemoryWritable()) {
                    if (checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_PUBLIC_EXTERNAL_STORAGE)) {
                        writeFile(WRITE_PUBLIC_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(this, R.string.external_memory_not_mounted, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /*
        Performs the required checks to read the file according to the selected source
    */
    public void loadFile(View view) {

        // Determine the source file to load according to the selected item
        switch (spinner.getSelectedItemPosition()) {

            // Application resources
            case 0:
                readFile(READ_RESOURCES);
                break;

            // Application internal storage
            case 1:
                readFile(READ_INTERNAL_STORAGE);
                break;

            // Application external storage
            case 2:
                // Check the external memory is readable
                // and that the user has granted permission for reading it (if API < 19)
                if (isExternalMemoryReadable()) {
                    if ((Build.VERSION.SDK_INT > 18) ||
                            (checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE))) {
                        readFile(READ_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(this, R.string.external_memory_not_mounted, Toast.LENGTH_SHORT).show();
                }
                break;

            // Public external storage (DOWNLOAD)
            case 3:
                // Check the external memory is readable and that the user has granted permission for reading it
                if (isExternalMemoryReadable()) {
                    if (checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, READ_PUBLIC_EXTERNAL_STORAGE)) {
                        readFile(READ_PUBLIC_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(this, R.string.external_memory_not_mounted, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /*
        Reads the source file and displays its contents in the available EditText
    */
    private void readFile(int operation) {

        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        String line;

        try {
            // Open the required Reader according to the selected source file
            switch (operation) {

                // Application resources
                case READ_RESOURCES:
                    reader = new BufferedReader(new InputStreamReader(
                            getResources().openRawResource(R.raw.app_resource_file)));
                    break;

                // Application internal storage
                case READ_INTERNAL_STORAGE:
                    reader = new BufferedReader(
                            new InputStreamReader(openFileInput("internal_storage_file")));
                    break;

                // Application external storage
                case READ_EXTERNAL_STORAGE:
                    reader = new BufferedReader(new FileReader(
                            new File(getExternalFilesDir(null), "external_storage_file")));
                    break;

                // Public external storage (DOWNLOAD)
                case READ_PUBLIC_EXTERNAL_STORAGE:
                    // Environment.DIRECTORY_DOCUMENTS is only available in API19+
                    if (Build.VERSION.SDK_INT > 18) {
                        reader = new BufferedReader(new FileReader(
                                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        "external_public_storage_file")));
                    } else {
                        reader = new BufferedReader(new FileReader(
                                new File(Environment.getExternalStorageDirectory() + "/Download",
                                        "external_public_storage_file")));
                    }
                    break;
            }

            if (reader != null) {
                // Append all lines read from the file into a StringBuilder
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            // Display the file contents in the EditText
            etFileContent.setText(builder.toString());

        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, R.string.io_file_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            try {
                // Ensure that the Reader is closed (if opened)
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        Write the EditText contents into the destination file
    */
    private void writeFile(int operation) {

        BufferedWriter writer = null;

        try {
            // Open the required Writer according to the selected destination file
            switch (operation) {

                // Application internal storage
                case WRITE_INTERNAL_STORAGE:
                    writer = new BufferedWriter(
                            new OutputStreamWriter(openFileOutput("internal_storage_file", MODE_PRIVATE)));
                    break;

                // Application external storage
                case WRITE_EXTERNAL_STORAGE:
                    writer = new BufferedWriter(new FileWriter(
                            new File(getExternalFilesDir(null), "external_storage_file")));
                    break;

                // Public external storage (DOWNLOAD)
                case WRITE_PUBLIC_EXTERNAL_STORAGE:
                    if (Build.VERSION.SDK_INT > 18) {
                        writer = new BufferedWriter(new FileWriter(
                                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        "external_public_storage_file")));
                    } else {
                        writer = new BufferedWriter(new FileWriter(
                                new File(Environment.getExternalStorageDirectory() + "/Download",
                                        "external_public_storage_file")));
                    }
                    break;
            }

            if (writer != null) {
                // Write to file
                writer.write(etFileContent.getText().toString());
                writer.flush();
            }

        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, R.string.io_file_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            try {
                // Ensure that the Writer is closed (if opened)
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        Checks that the external memory is writable (mounted)
    */
    private boolean isExternalmemoryWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /*
        Checks that the external memory is readable (mounted or mounter_read_only)
    */
    private boolean isExternalMemoryReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /*
        Checks that the user has granted permission to access the external memory in read/write mode.
        Otherwise, it launches a dialog to ask the user to grant permission
    */
    private boolean checkPermission(String permission, int operation) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, permission)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, operation);
            return false;
        }
    }

    /*
        This method is called whenever the user dismisses the dialog used to ask for permissions.
        Checks whether the user has granted the required permissions and acts accordingly.
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Check that permissions have been granted
        if ((grantResults.length > 0) && (PackageManager.PERMISSION_GRANTED == grantResults[0])) {

            // Launch the required operation according to the received requestCode
            switch (requestCode) {

                // Read from application external storage
                case READ_EXTERNAL_STORAGE:
                    readFile(READ_EXTERNAL_STORAGE);
                    break;

                // Read from public external storage (DOWNLOAD)
                case READ_PUBLIC_EXTERNAL_STORAGE:
                    readFile(READ_PUBLIC_EXTERNAL_STORAGE);
                    break;

                // Write to application external storage
                case WRITE_EXTERNAL_STORAGE:
                    writeFile(WRITE_EXTERNAL_STORAGE);
                    break;

                // Write to public external storage (DOWNLOAD)
                case WRITE_PUBLIC_EXTERNAL_STORAGE:
                    writeFile(WRITE_PUBLIC_EXTERNAL_STORAGE);
                    break;
            }
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }
}
