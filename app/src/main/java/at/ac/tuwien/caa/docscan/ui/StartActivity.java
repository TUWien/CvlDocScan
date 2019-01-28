/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   21. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.firebase.FirebaseApp;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;


/**
 * Activity called after the app is started. This activity is responsible for requesting the camera
 * permission. If the permission is given the CameraActivity is started via an intent.
 * Based on this example: <a href="https://github.com/googlesamples/android-RuntimePermissionsBasic">android-RuntimePermissionsBasic
 </a>
 */
public class StartActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private AlertDialog mAlertDialog;
//    private static final String CLASS_NAME = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_container_view);

        askForPermissions();

        //initialize Firebase for OCR
        FirebaseApp.initializeApp(this);

    }

    /**
     * Asks for permissions that are really needed. If they are not given, the app is unusable.
     */
    private void askForPermissions() {

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        else
            startCamera();

    }

    /**
     * Ask for multiple permissions. Taken from:
     * https://stackoverflow.com/a/34343101/9827698
     * @param context
     * @param permissions
     * @return
     */
    public static boolean hasPermissions(Context context, String... permissions) {

        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }

            return true;
        }

        return false;

    }


    private void showPermissionRequiredAlert(String alertText) {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertText += "\n" + getResources().getString(R.string.start_permission_retry_text);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.start_permission_title)
                .setPositiveButton(R.string.start_confirm_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
//                        Restart the activity:
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);

                    }
                })
                .setNegativeButton(R.string.start_cancel_button_text, null)
                .setCancelable(true)
                .setMessage(alertText);

        // create alert dialog
        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();

        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

//        Note: we ignore here the values passed in grantResults and ask directly for
//        checkSelfPermissions, although this is a bad style. The reason for this is that if the app
//        is killed by the system, the grantResults provided are not correct, because the
//        permissions are asked again:

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)) {
            showPermissionRequiredAlert(getResources().getString(
                    R.string.start_permission_camera_text));
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            showPermissionRequiredAlert(getResources().getString(
                    R.string.start_permission_storage_text));
            return;
        }

//        If CAMERA and WRITE_EXTERNAL_STORAGE permissions are given start the camera, the GPS
//        permissions are not required:
        startCamera();

    }

    private void startCamera() {

        DocumentStorage.loadJSON(this);
        SyncStorage.loadJSON(this);

        Intent intent = new Intent(this, CameraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();

    }


}
