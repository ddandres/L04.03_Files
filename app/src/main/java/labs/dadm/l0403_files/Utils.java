/*
 * Copyright (c) 2021. David de Andrés and Juan Carlos Ruiz, DISCA - UPV, Development of apps for mobile devices.
 */

package labs.dadm.l0403_files;

public class Utils {

    // Constants identifying the keys for the adapter
    public static final String IMAGE = "Image";
    public static final String NAME = "Name";

    // Constants identifying the current operation
    public static final int READ_RESOURCES = 0;
    public static final int READ_INTERNAL_STORAGE = 1;
    public static final int READ_PRIVATE_EXTERNAL_STORAGE = 2;
    public static final int READ_PUBLIC_MEDIA_STORAGE = 3;
    public static final int READ_PUBLIC_OTHER_STORAGE = 4;
    public static final int WRITE_INTERNAL_STORAGE = 5;
    public static final int WRITE_PUBLIC_MEDIA_STORAGE = 6;
    public static final int WRITE_PRIVATE_EXTERNAL_STORAGE = 7;
    public static final int WRITE_PUBLIC_OTHER_STORAGE = 8;

    // Constants identifying the selection in the Spinner
    public static final int RESOURCES = 0;
    public static final int INTERNAL_STORAGE = 1;
    public static final int PRIVATE_EXTERNAL_STORAGE = 2;
    public static final int PUBLIC_MEDIA_STORAGE = 3;
    public static final int PUBLIC_OTHER_STORAGE = 4;

    // Constant defining the date and time format to be used as a timestamp
    public static final String DATE_TIME_FORMAT = "_ddMMyy_HHmmss";

}
