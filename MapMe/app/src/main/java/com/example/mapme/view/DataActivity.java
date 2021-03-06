package com.example.mapme.view;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.mapme.R;
import com.example.mapme.model.AppService;
import com.example.mapme.model.GeoObject;
import com.example.mapme.presenter.DataPresenter;

import java.util.HashMap;

/**
 * DataActivity - Activity to display data from database.
 */
public class DataActivity extends AppCompatActivity {

    private DataPresenter presenter;
    public AppService appService;
    protected boolean appServiceBound;
    private boolean serviceConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        presenter = new DataPresenter(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(this, AppService.class);
        bindService(bindIntent, appServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(appServiceConnection);
    }

    /**
     * AppService Connection.
     */
    public ServiceConnection appServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppService.LocalBinder binder = (AppService.LocalBinder) service;
            appService = binder.getService();
            appServiceBound = true;
            appService.registerListener(presenter);
            serviceConnected = true;
            Log.i("info", "Service bound to DataActivity.");
            presenter.getData();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            appServiceBound = false;
            Log.i("info", "Service unbound to DataActivity.");
        }
    };

    /**
     * Cancel and go back to previous activity.
     *
     * @param view
     */
    public void cancel(View view) {
        this.finish();
    }

    /**
     * Calls presenter to save file to firebase storage.
     *
     * @param view
     */
    public void saveToCloud(View view) {
        presenter.saveToCloud();
    }

    /**
     * Displays data from database.
     *
     * @param objects
     */
    public void displayData(HashMap<String, GeoObject> objects) {
        TableLayout inputFields = findViewById(R.id.inputFields);
        inputFields.removeAllViews();
        for (final String objectKey : objects.keySet()) {
            GeoObject object = objects.get(objectKey);
            // id and type
            TableRow tableRowObject = new TableRow(this);
            TextView textViewObject = new TextView(this);
            textViewObject.setText("  " + object.getProperties().get("type") + ": " + objectKey + "    ");
            textViewObject.setTypeface(null, Typeface.BOLD);
            tableRowObject.addView(textViewObject);
            // edit button
            ImageButton edit = new ImageButton(this);
            edit.setImageDrawable(getResources().getDrawable(R.drawable.edit));
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startEditObjectActivity(objectKey);
                }
            });
            tableRowObject.addView(edit);
            // delete button
            ImageButton delete = new ImageButton(this);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.delete));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.deleteObject(objectKey);
                }
            });
            tableRowObject.addView(delete);
            inputFields.addView(tableRowObject);
            // properties
            for (final String propertyKey : object.getProperties().keySet()) {

                TableRow tableRowProperties = new TableRow(this);
                String key = propertyKey;
                String value = object.getProperties().get(propertyKey);
                TextView textViewProperties = new TextView(this);
                textViewProperties.setText("     " + key + " - " + value);
                tableRowProperties.addView(textViewProperties);
                inputFields.addView(tableRowProperties);
            }
            // empty rows
            inputFields.addView(createEmptyRow());
            inputFields.addView(createEmptyRow());
            inputFields.addView(createEmptyRow());
            inputFields.addView(createEmptyRow());
        }
    }

    /**
     * Creates an empty row.
     *
     * @return TableRow
     */
    public TableRow createEmptyRow() {
        TableRow emptyRow = new TableRow(this);
        TextView emptytextView = new TextView(this);
        emptytextView.setText(" ");
        emptyRow.addView(emptytextView);
        return emptyRow;
    }


    /**
     * Opens new EditInformationActivity for geoObject with given id.
     *
     * @param id
     */
    public void startEditObjectActivity(String id) {
        Intent intent = new Intent(this, EditInformationActivity.class);
        intent.putExtra("name", "Edit Object");
        intent.putExtra("id", id);
        startActivity(intent);
    }

    /**
     * Shows info dialog to reset database.
     *
     * @param view
     */
    public void showInfoResetDatabase(View view) {
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
        infoDialog.setTitle("Reset Database");
        infoDialog.setMessage("Do you really want to reset the database? All previously saved data will be lost. ");
        infoDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.setNeutralButton("Reset",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.resetDatabase();
                        dialog.cancel();
                    }
                });
        infoDialog.show();
    }

    /**
     * Shows info dialog when upload is completed.
     */
    public void showInfoUploadSuccessful() {
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
        infoDialog.setTitle("Success");
        infoDialog.setMessage("Database was saved to Firebase Storage. ");
        infoDialog.setNegativeButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.show();
    }

}