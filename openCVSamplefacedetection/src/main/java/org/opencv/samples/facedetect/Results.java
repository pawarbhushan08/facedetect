package org.opencv.samples.facedetect;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Results extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        final TextView textViewToChange = (TextView) findViewById(R.id.editText);
        textViewToChange.setText("Ready to show the result");
    }
}
