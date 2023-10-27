package com.example.app8;

import android.content.Intent;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    public static HashMap<String, FaceClassifier.Recognition> registered = new HashMap<>();


    Button registerBtn,recognizeBtn;
    private Connection connection = SQLConnection.getConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBtn = findViewById(R.id.buttonregister);
        recognizeBtn = findViewById(R.id.buttonrecognize);


        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,RegisterActivity.class));
            }
        });

        recognizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,RecognitionActivity.class));
                registered = getRegisteredFacesFromDatabase();
            }
        });
    }

    public HashMap<String, FaceClassifier.Recognition> getRegisteredFacesFromDatabase() {
        // Create an AsyncTask to perform the database query asynchronously
        new AsyncTask<Void, Void, HashMap<String, FaceClassifier.Recognition>>() {
            @Override
            protected HashMap<String, FaceClassifier.Recognition> doInBackground(Void... voids) {
                HashMap<String, FaceClassifier.Recognition> registeredFaces = new HashMap<>();

                if (connection != null) {
                    String query = "SELECT sdName, Id, Title, Distance, Embeeding FROM registered";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            while (resultSet.next()) {
                                String sdName = resultSet.getString("sdName");
                                String id = resultSet.getString("Id");
                                String title = resultSet.getString("Title");
                                float distance = resultSet.getFloat("Distance");
                                String embeedingJson = resultSet.getString("Embeeding");

                                // Deserialize the embeeding JSON into the appropriate object.
                                // You may need to use Gson or another library to do this.
                                float[][] embeeding = new Gson().fromJson(embeedingJson, float[][].class);
                                FaceClassifier.Recognition rec = new FaceClassifier.Recognition(id, title, distance, new RectF(), embeeding);

                                registeredFaces.put(sdName, rec);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                return registeredFaces;
            }

            @Override
            protected void onPostExecute(HashMap<String, FaceClassifier.Recognition> result) {
                // Handle the result after the database query is complete
                // Set the registered faces and count down the latch
                registered = result;
                Log.d("registeredPostMain", String.valueOf(registered.size()));
                //registeredLatch.countDown();
            }
        }.execute();

        // Return an empty HashMap immediately; the actual result will be set in onPostExecute
        return new HashMap<>();
    }
}