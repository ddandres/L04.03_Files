/*
 * Copyright (c) 2019. David de Andrés and Juan Carlos Ruiz, DISCA - UPV, Development of apps for mobile devices.
 */

package labs.dadm.l0403_files;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Hold references to View
    Spinner spinner;
    EditText etFileContent;
    Button bSave;
    GridView gvImages;
    SimpleAdapter adapter;

    ArrayList<Map<String, String>> list;

    ActivityResultLauncher<Intent> launcherReadPublicOther;
    ActivityResultLauncher<Intent> launcherWritePublicOther;
    ActivityResultLauncher<String> launcherRequestPermission;

    int operation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get references to the View
        etFileContent = findViewById(R.id.etFileContent);
        bSave = findViewById(R.id.bSave);
        spinner = findViewById(R.id.spinner);
        gvImages = findViewById(R.id.gvImages);

        // Save file to the selected storage space when the button is clicked
        findViewById(R.id.bSave).setOnClickListener(v -> saveFile());

        // Continue with reading the file from public external other storage
        launcherReadPublicOther = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        readPublicOtherStorage(result.getData());
                    }
                });

        // Continue with writing the file to public external other storage
        launcherWritePublicOther = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        writePublicOtherStorage(result.getData());
                    }
                });

        // This callback is called whenever the user dismisses the dialog used to ask for permissions.
        // Checks whether the user has granted the required permissions and acts accordingly.
        launcherRequestPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                permissionGranted -> {
                    if (permissionGranted) {
                        // Launch the required operation according to the received requestCode
                        switch (operation) {
                            case Utils.READ_PRIVATE_EXTERNAL_STORAGE: // Read from application external storage
                            case Utils.READ_PUBLIC_MEDIA_STORAGE: // Read from public external storage (MEDIA)
                            case Utils.READ_PUBLIC_OTHER_STORAGE: // Read from public external storage
                                readFile(operation);
                                break;

                            case Utils.WRITE_PRIVATE_EXTERNAL_STORAGE: // Write to application external storage
                            case Utils.WRITE_PUBLIC_MEDIA_STORAGE: // Write to public media storage (Images)
                            case Utils.WRITE_PUBLIC_OTHER_STORAGE: // Write to public external storage
                                writeFile(operation);
                                break;
                        }
                    } else {
                        Toast.makeText(MainActivity.this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Load data from internal/external storage when an item is selected in the Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // Clear the list of images for external public storage (Images)
                list.clear();
                adapter.notifyDataSetChanged();

                // Load the default file for the selected element
                loadFile(position);

                // Resources cannot be overwritten,
                // so the Save button is disabled when Resources are selected
                bSave.setEnabled(position != Utils.RESOURCES);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Create the list that stores the information to be displayed on the GridView
        list = new ArrayList<>();
        // Adapter to create the Views that display in the GridView the information contained
        // in the list (URI to the Image and name of the file)
        adapter = new SimpleAdapter(MainActivity.this, list, R.layout.grid_element,
                new String[]{Utils.IMAGE, Utils.NAME}, new int[]{R.id.ivImage, R.id.tvImage});
        // Set the adapter to the GridView
        gvImages.setAdapter(adapter);

    }

    // Performs the required checks to read the file according to the selected destination
    private void saveFile() {
        // Determine the destination file to load according to the selected item
        switch (spinner.getSelectedItemPosition()) {
            // Application internal storage
            case Utils.INTERNAL_STORAGE:
                writeFile(Utils.WRITE_INTERNAL_STORAGE);
                break;
            // Application external storage
            case Utils.PRIVATE_EXTERNAL_STORAGE:
                checkPermissionWritePrivateExternalStorage();
                break;
            // Public media storage (Images)
            case Utils.PUBLIC_MEDIA_STORAGE:
                checkPermissionWritePublicMediaStorage();
                break;
            // Public external other storage (Storage Access Framework)
            case Utils.PUBLIC_OTHER_STORAGE:
                checkPermissionWritePublicOtherStorage();
                break;
        }
    }

    // Checks the external memory is writable
    // and that the user has granted permission for writing it (if API < 19)
    private void checkPermissionWritePrivateExternalStorage() {
        if (isExternalmemoryWritable()) {
            if ((Build.VERSION.SDK_INT > 18) ||
                    checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Utils.WRITE_PRIVATE_EXTERNAL_STORAGE)) {
                writeFile(Utils.WRITE_PRIVATE_EXTERNAL_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Checks the external memory is writable and
    // that the user has granted permission for writing it (if API < 29)
    private void checkPermissionWritePublicMediaStorage() {
        if (isExternalmemoryWritable()) {
            if ((Build.VERSION.SDK_INT > 28) ||
                    checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Utils.WRITE_PUBLIC_MEDIA_STORAGE)) {
                writeFile(Utils.WRITE_PUBLIC_MEDIA_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Checks the external memory is writable and
    // that the user has granted permission for writing it (if API < 19)
    private void checkPermissionWritePublicOtherStorage() {
        if (isExternalmemoryWritable()) {
            if (Build.VERSION.SDK_INT < 19) {
                if (checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Utils.WRITE_PUBLIC_OTHER_STORAGE)) {
                    writeFile(Utils.WRITE_PUBLIC_OTHER_STORAGE);
                }
            } else {
                // API < 19 then directly write to external storage
                writeFile(Utils.WRITE_PUBLIC_OTHER_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Performs the required checks to read the file according to the selected source
    private void loadFile(int position) {
        // Determine the source file to load according to the selected item
        switch (position) {
            // Application resources
            case Utils.RESOURCES:
                readFile(Utils.READ_RESOURCES);
                break;
            // Application internal storage
            case Utils.INTERNAL_STORAGE:
                readFile(Utils.READ_INTERNAL_STORAGE);
                break;
            // Application external storage
            case Utils.PRIVATE_EXTERNAL_STORAGE:
                checkPermissionReadPrivateExternalStorage();
                break;
            // Public media storage (Images)
            case Utils.PUBLIC_MEDIA_STORAGE:
                checkPermissionReadPublicMediaStorage();
                break;
            // Public external storage (Storage Access Framework)
            case Utils.PUBLIC_OTHER_STORAGE:
                checkPermissionReadPublicOtherMedia();
                break;
        }
    }

    // Checks the external memory is readable and, if required,
    // that the user has granted permission for reading it (if API < 19)
    private void checkPermissionReadPublicOtherMedia() {
        if (isExternalMemoryReadable()) {
            if ((Build.VERSION.SDK_INT > 19) ||
                    checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Utils.READ_PUBLIC_OTHER_STORAGE)) {
                readFile(Utils.READ_PUBLIC_OTHER_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Checks the external memory is readable and, if required,
    // that the user has granted permission for reading it (if API < 29)
    private void checkPermissionReadPublicMediaStorage() {
        if (isExternalMemoryReadable()) {
            if ((Build.VERSION.SDK_INT > 28) ||
                    checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Utils.READ_PUBLIC_MEDIA_STORAGE)) {
                readFile(Utils.READ_PUBLIC_MEDIA_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Checks the external memory is readable
    // and that the user has granted permission for reading it (if API < 19)
    private void checkPermissionReadPrivateExternalStorage() {
        if (isExternalMemoryReadable()) {
            if ((Build.VERSION.SDK_INT > 18) ||
                    (checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Utils.READ_PRIVATE_EXTERNAL_STORAGE))) {
                readFile(Utils.READ_PRIVATE_EXTERNAL_STORAGE);
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.external_memory_not_mounted,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Reads the source file and displays its contents in the available EditText
    private void readFile(int operation) {

        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        String line;

        try {
            // Open the required Reader according to the selected source file
            switch (operation) {

                // Application resources
                case Utils.READ_RESOURCES:
                    reader = new BufferedReader(new InputStreamReader(
                            getResources().openRawResource(R.raw.app_resource_file)));
                    break;

                // Application internal storage
                case Utils.READ_INTERNAL_STORAGE:
                    reader = new BufferedReader(
                            new InputStreamReader(openFileInput("internal_storage_file")));
                    break;

                // Application external storage
                case Utils.READ_PRIVATE_EXTERNAL_STORAGE:
                    reader = new BufferedReader(new FileReader(
                            new File(getExternalFilesDir(null), "external_storage_file")));
                    break;

                // Public Media storage (Images)
                case Utils.READ_PUBLIC_MEDIA_STORAGE:
                    readAndDisplayImageFromPublicMediaStorage();
                    break;

                // Public other storage (Storage Access Framework)
                case Utils.READ_PUBLIC_OTHER_STORAGE:
                    // Use the Storage Access Framework
                    if (Build.VERSION.SDK_INT > 18) {
                        // Use the default application from the device to open the document
                        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        // Request that the resulting URI can be opened with openFileDescriptor()
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        // Set the MIME type
                        intent.setType("text/plain");
                        // Launch the most suitable application
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            launcherReadPublicOther.launch(intent);
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    R.string.no_app_available,
                                    Toast.LENGTH_SHORT).show();
                        }
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
            etFileContent.setText("");
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, R.string.io_file_error, Toast.LENGTH_SHORT).show();
            etFileContent.setText("");
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

    private void readAndDisplayImageFromPublicMediaStorage() {
        // Columns to retrieve from the table
        final String[] projection = {MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME};
        // When the MIME_TYPE for the entry is
        final String selection = MediaStore.Images.Media.MIME_TYPE + " = ?";
        // image/png
        final String[] arguments = {"image/png"};
        final String order = MediaStore.Images.Media.DISPLAY_NAME + " ASC";

        try {
            // Query the ContentProvider to get the desired entries from the table
            Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    arguments,
                    order
            );

            // Process the information retrieved (if any)
            if (cursor != null) {
                Uri imageUri;
                HashMap<String, String> map;
                // Access the next entry
                while (cursor.moveToNext()) {
                    // Get the image URI
                    imageUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getInt(0));
                    // Add the Image URI and its file name to the list
                    map = new HashMap<>();
                    map.put(Utils.IMAGE, imageUri.toString());
                    map.put(Utils.NAME, cursor.getString(1));
                    list.add(map);
                }
                // Close the cursor
                cursor.close();
                // Notify the adapter to refresh the GridView with the new information
                adapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Write the EditText contents into the destination file
    private void writeFile(int operation) {

        BufferedWriter writer = null;
        final SimpleDateFormat dateFormat = new SimpleDateFormat(Utils.DATE_TIME_FORMAT, Locale.US);

        try {
            // Open the required Writer according to the selected destination file
            switch (operation) {

                // Application internal storage
                case Utils.WRITE_INTERNAL_STORAGE:
                    writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    openFileOutput("internal_storage_file", MODE_PRIVATE)));
                    break;

                // Application external storage
                case Utils.WRITE_PRIVATE_EXTERNAL_STORAGE:
                    writer = new BufferedWriter(new FileWriter(
                            new File(getExternalFilesDir(null),
                                    "external_storage_file")));
                    break;

                // Public media storage (Images)
                case Utils.WRITE_PUBLIC_MEDIA_STORAGE:
                    writeImageToPublicMediaStorage();
                    break;

                // Public external storage (Storage Access Framework)
                case Utils.WRITE_PUBLIC_OTHER_STORAGE:

                    // Use the Storage Access Framework
                    if (Build.VERSION.SDK_INT > 18) {
                        // Use the default application from the device to create the document
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        // Request that the resulting URI can be opened with openFileDescriptor()
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        // Set the MIME type
                        intent.setType("text/plain");
                        // Set the file name
                        intent.putExtra(Intent.EXTRA_TITLE,
                                "public_other_storage" + dateFormat.format(new Date()) + ".txt");
                        // Launch the most suitable application
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            launcherWritePublicOther.launch(intent);
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    R.string.no_app_available,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // API < 19 then directly write to external storage
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

        } catch (
                FileNotFoundException e) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (
                IOException e) {
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

    private void writeImageToPublicMediaStorage() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(Utils.DATE_TIME_FORMAT, Locale.US);

        // Set the information to be stored in the ContentProvider
        final ContentValues values = new ContentValues();
        // Format the current time as desired
        // Set the file name
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "andy" + dateFormat.format(new Date()) + ".png");
        // Set the MIME_TYPE
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        // If API > 28 mark the entry as not inserted yet, so other apps cannot access it
        if (Build.VERSION.SDK_INT > 28) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        // Get access to the content model
        final ContentResolver resolver = getContentResolver();
        // Insert the selected image into the table
        final Uri imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // Show a message in case something went wrong
        if (imageUri == null) {
            Toast.makeText(
                    MainActivity.this,
                    R.string.mediastore_error,
                    Toast.LENGTH_SHORT).show();
        } else {
            // The returned URI is used to store the image file
            OutputStream os = null;
            try {
                // Get the image to be written from raw resources
                final Bitmap bitmap = BitmapFactory.decodeStream(
                        getResources().openRawResource(R.raw.andy));
                // Open an OutputStream using the return URI
                os = resolver.openOutputStream(imageUri);
                // Write the image to the OutputStream
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            } catch (FileNotFoundException fnfe) {
                Toast.makeText(
                        MainActivity.this,
                        R.string.file_not_found,
                        Toast.LENGTH_SHORT).show();
            } finally {
                if (os != null) {
                    try {
                        // If opened, close the Outputstream
                        os.close();

                        // If API > 28 then clear the IS_PENDING flag
                        // for other apps to access this entry
                        if (Build.VERSION.SDK_INT > 28) {
                            values.clear();
                            values.put(MediaStore.Images.Media.IS_PENDING, 0);
                            resolver.update(imageUri, values, null, null);
                        }

                        // Update the GridView with the new element
                        list.clear();
                        readFile(Utils.READ_PUBLIC_MEDIA_STORAGE);

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }

        }
    }

    // Create file in public external storage (Storage Access Framework)
    private void writePublicOtherStorage(Intent data) {
        try {
            if (data != null) {
                // Get a file descriptor to write data in the provided URI
                final ParcelFileDescriptor pfd =
                        getContentResolver().openFileDescriptor(data.getData(), "w");
                // FileOutputStream to write to the FileDescriptor
                final FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                // Write the content of the EditText as bytes
                fos.write(etFileContent.getText().toString().getBytes());
                // Flush and close the streams and descriptors
                fos.flush();
                fos.close();
                pfd.close();
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // Read file from public external storage (Storage Access Framework)
    private void readPublicOtherStorage(Intent data) {
        try {
            if (data != null) {
                StringBuilder builder = new StringBuilder();
                String line;
                // Get a file descriptor to read data from the provided URI
                final ParcelFileDescriptor pfd =
                        getContentResolver().openFileDescriptor(data.getData(), "r");
                // BufferedReader to read from the FileDescriptor
                final BufferedReader reader =
                        new BufferedReader(new FileReader(pfd.getFileDescriptor()));
                // Get the content of the file line by line
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                // Update the EditText with the contents of the file
                etFileContent.setText(builder.toString());
                // Close the reader and descriptor
                reader.close();
                pfd.close();
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // Checks that the external memory is writable (mounted)
    private boolean isExternalmemoryWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    // Checks that the external memory is readable (mounted or mounter_read_only)
    private boolean isExternalMemoryReadable() {
        final String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    // Checks that the user has granted permission to access the external memory in read/write mode.
    // Otherwise, it launches a dialog for the user to grant permission
    private boolean checkPermission(String permission, int operation) {
        if (PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, permission)) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // AlertDialog.Builder to help create a custom dialog
                final AlertDialog.Builder builder =
                        new AlertDialog.Builder(MainActivity.this);
                // Set the title of the dialog
                builder.setTitle(R.string.rationale_title);
                // Set the message to be displayed
                builder.setMessage(R.string.rationale_message);
                // Set a button for a positive action
                builder.setPositiveButton(
                        android.R.string.yes,
                        (dialog, which) -> ActivityCompat.requestPermissions(
                                this, new String[]{permission}, operation));
                // Prevent the dialog from being cancelled
                builder.setCancelable(false);
                // Create the dialog
                final AlertDialog dialog = builder.create();
                // Show the dialog
                dialog.show();
            } else {
                this.operation = operation;
                launcherRequestPermission.launch(permission);
            }
            return false;
        }
    }

}
