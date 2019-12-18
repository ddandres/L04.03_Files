# L04.03_Files
Lecture 04.03 Files, DISCA - UPV, Development of apps for mobile devices.

Shows how to read/write files from/to different sources/destinations:
- Application resources (read only)
- Application internal storage
- Application external storage
- Public external media storage through ContentProvider
- Other public external storage through Storage Access Framework

It is necessary to check that the external memory is available in read/write mode before accessing it.

Also, permissions must be checked (in some cases) to access public external storage. 
If permissions are not granted, a dialog is shown to the user to ask for them.
