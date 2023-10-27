package com.example.app8;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class StudentListActivityTeacher extends AppCompatActivity {
    private Context context;
    private ListView listView;
    private CustomStudentListAdapter adapter; // Sử dụng CustomStudentListAdapter
    private ArrayList<String> studentList;
    private ArrayList<byte[]> imageDataList; // Danh sách dữ liệu hình ảnh
    private Button imageButton;
    private Button videoButton;
    private Connection connection;

    private boolean isMultipleChoiceMode = false;

    private final int ADD_STUDENT_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list_teacher);
        context = this;

        // Kết nối đến cơ sở dữ liệu bằng SQLConnection
        connection = SQLConnection.getConnection();
        TextView titleTextView = findViewById(R.id.titleTextView);
        listView = findViewById(R.id.listView);
        imageButton = findViewById(R.id.imageButton);
        videoButton = findViewById(R.id.videoButton);
        studentList = new ArrayList<>();
        imageDataList = new ArrayList<>(); // Khởi tạo danh sách dữ liệu hình ảnh
        adapter = new CustomStudentListAdapter(this, studentList, imageDataList); // Sử dụng CustomStudentListAdapter
        listView.setAdapter(adapter);

        // Nhận tên môn học từ Intent
        String subjectName = getIntent().getStringExtra("SubjectName");
        titleTextView.setText(subjectName);

        int backgroundValue = getBackgroundValueFromDatabase(subjectName);
        // Chuyển giá trị background thành tên tài nguyên drawable
        String drawableName = "background_" + backgroundValue; // Ví dụ: background_1
        // Lấy ID tài nguyên drawable từ tên
        int backgroundResId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        titleTextView.setBackgroundResource(backgroundResId);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Kiểm tra xem có phải là chế độ CHOICE_MODE_MULTIPLE không
                if (!isMultipleChoiceMode) {
                    // Lấy tên sinh viên khi bạn bấm vào item trong ListView
                    String studentName = studentList.get(position);

                    // Truy vấn cơ sở dữ liệu để lấy thông tin sinh viên dựa vào tên
                    StudentInfo studentInfo = getStudentInfoFromDatabase(studentName);

                    // Kiểm tra nếu có dữ liệu sinh viên
                    if (studentInfo != null) {
                        // Tạo một AlertDialog.Builder
                        AlertDialog.Builder builder = new AlertDialog.Builder(StudentListActivityTeacher.this);
                        builder.setTitle("Thông tin sinh viên");

                        // Tạo một View để hiển thị thông tin sinh viên
                        View dialogView = getLayoutInflater().inflate(R.layout.student_info_dialog, null);

                        // Lấy các thành phần View trong dialogView
                        TextView nameTextView = dialogView.findViewById(R.id.nameTextView);
                        TextView dobTextView = dialogView.findViewById(R.id.dobTextView);
                        TextView codeTextView = dialogView.findViewById(R.id.codeTextView);
                        ImageView imageView = dialogView.findViewById(R.id.imageView);

                        // Đặt thông tin sinh viên vào các View
                        nameTextView.setText(studentInfo.getName());
                        dobTextView.setText(studentInfo.getDateOfBirth());
                        codeTextView.setText(studentInfo.getCode());
                        byte[] imageData = studentInfo.getImageData();
                        if (imageData != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            imageView.setImageBitmap(bitmap);
                        }

                        // Đặt View vào AlertDialog
                        builder.setView(dialogView);

                        // Tạo và hiển thị AlertDialog
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                }
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentListActivityTeacher.this, MainActivity.class));
            }
        });
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StudentListActivityTeacher.this, RecognizeActivity.class));
            }
        });


        // Tải danh sách sinh viên từ cơ sở dữ liệu dựa trên tên môn học
        loadStudentList(subjectName);
    }

    private boolean deleteStudent(String studentName) {
        if (connection != null) {
            try {
                // Thực hiện truy vấn SQL để xóa sinh viên dựa trên tên
                String query = "DELETE FROM STUDENT_LIST WHERE name_student = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, studentName);

                int rowsDeleted = preparedStatement.executeUpdate();
                preparedStatement.close();

                return rowsDeleted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // Hàm này thực hiện tải danh sách sinh viên từ cơ sở dữ liệu và cập nhật ListView
    private void loadStudentList(String subjectName) {
        if (connection != null) {
            try {
                studentList.clear();
                imageDataList.clear(); // Xóa danh sách dữ liệu hình ảnh
                String query = "SELECT name_student, ImageData FROM STUDENT_LIST WHERE class_id IN (SELECT id FROM CLASS WHERE name_subject = ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, subjectName);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    String studentName = resultSet.getString("name_student");
                    studentList.add(studentName);

                    byte[] imageData = resultSet.getBytes("ImageData");
                    imageDataList.add(imageData); // Thêm dữ liệu hình ảnh vào danh sách
                }
                resultSet.close();
                preparedStatement.close();
                adapter.notifyDataSetChanged();
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lấy dữ liệu từ cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không thể kết nối đến cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }

    private int getBackgroundValueFromDatabase(String className) {
        int backgroundValue = -1; // Giá trị mặc định

        if (connection != null) {
            try {
                // Truy vấn SQL để lấy background từ bảng CLASS dựa trên tên lớp học
                String getBackgroundQuery = "SELECT background FROM CLASS WHERE name_subject = ?";
                PreparedStatement getBackgroundStatement = connection.prepareStatement(getBackgroundQuery);
                getBackgroundStatement.setString(1, className);
                ResultSet backgroundResult = getBackgroundStatement.executeQuery();

                // Kiểm tra xem có dữ liệu trả về không
                if (backgroundResult.next()) {
                    backgroundValue = backgroundResult.getInt("background");
                }

                getBackgroundStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return backgroundValue;
    }
    private StudentInfo getStudentInfoFromDatabase(String studentName) {
        StudentInfo studentInfo = null;

        if (connection != null) {
            try {
                // Truy vấn SQL để lấy thông tin sinh viên từ bảng STUDENT_LIST dựa trên tên sinh viên
                String query = "SELECT name_student, code_student, date_of_birth, ImageData FROM STUDENT_LIST WHERE name_student = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, studentName);
                ResultSet resultSet = preparedStatement.executeQuery();

                // Kiểm tra xem có dữ liệu trả về không
                if (resultSet.next()) {
                    String name = resultSet.getString("name_student");
                    String code = resultSet.getString("code_student");
                    String dateOfBirth = resultSet.getString("date_of_birth");
                    byte[] imageData = resultSet.getBytes("ImageData");

                    // Tạo đối tượng StudentInfo từ dữ liệu lấy được
                    studentInfo = new StudentInfo(name, code, dateOfBirth, imageData);
                }

                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return studentInfo;
    }


    void loadAndUpdateStudentList(String subjectName) {
        loadStudentList(subjectName);
    }
}
