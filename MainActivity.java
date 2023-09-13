package com.example.imageconverter;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 123;

    private Button btnSelectImage;
    private Button btnCaptureImage;
    private RadioButton radioJpg;
    private RadioButton radioJpeg;
    private RadioButton radioPdf;
    private RadioButton radiopng;
    private RadioGroup radioGroup;
    private EditText fileNameEditText;
    private Button btnConvert;
    private Button btnDownload;
    private Bitmap selectedImage;
    private File convertedFile;
    private String customFileName = "";

    private ProgressBar uploadProgressBar;
    private TextView uploadPercentageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        radioJpg = findViewById(R.id.radioJpg);
        radiopng = findViewById(R.id.radiopng);
        radioGroup = findViewById(R.id.radioGroup);
        fileNameEditText=findViewById(R.id.fileNameEditText);
        radioJpeg = findViewById(R.id.radioJpeg);
        radioPdf = findViewById(R.id.radioPdf);
        btnConvert = findViewById(R.id.btnConvert);
        btnDownload = findViewById(R.id.btnDownload);

        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        uploadPercentageTextView = findViewById(R.id.uploadPercentageTextView);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkCameraPermission()) {
                    captureImage();
                } else {
                    requestCameraPermission();
                }
            }
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View view) {
                                              if (selectedImage != null) {
                                                  handleCustomFileName();
                                                  convertAndSaveImage();
                                              } else {
                                                  Toast.makeText(getApplicationContext(), "Please select or capture an image first.", Toast.LENGTH_SHORT).show();
                                              }
                                          }

            private void handleCustomFileName() {
                EditText fileNameEditText = findViewById(R.id.fileNameEditText);
                customFileName = fileNameEditText.getText().toString().trim();

                if (!customFileName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Custom file name set: " + customFileName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Custom file name is empty.", Toast.LENGTH_SHORT).show();
                }
            }

        });
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (convertedFile != null) {

                    requestStoragePermission();
                    saveFileToExternalStorage(convertedFile);


//                    downloadFile(convertedFile);
                } else {
                    Toast.makeText(getApplicationContext(), "No file to download.", Toast.LENGTH_SHORT).show();
                }
            }

            private void requestStoragePermission() {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STORAGE_PERMISSION_REQUEST_CODE);
                } else {
                    saveFileToExternalStorage(convertedFile);
                }
            }


            private void saveFileToExternalStorage(File file) {
                if (isExternalStorageWritable()) {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        if (!downloadsDir.mkdirs()) {
                            Toast.makeText(getApplicationContext(), "Failed to create download directory", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    File outputFile = new File(downloadsDir, file.getName());
                    try {
                        FileInputStream inputStream = new FileInputStream(file);
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        inputStream.close();
                        outputStream.close();
                        Toast.makeText(getApplicationContext(), "File saved to Downloads folder", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "External storage is not available or writable", Toast.LENGTH_SHORT).show();
                }
            }

            private boolean isExternalStorageWritable() {
                String state = Environment.getExternalStorageState();
                return Environment.MEDIA_MOUNTED.equals(state);
            }

        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(getApplicationContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                try {
                    selectedImage = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                    btnConvert.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    selectedImage = (Bitmap) extras.get("data");
                    btnConvert.setEnabled(true);
                }
            }
        }
    }

    private void convertAndSaveImage() {

        File outputDir = getExternalCacheDir();
        if (outputDir != null) {
            File outputFile;

            String filePath = constructFilePath();

            outputFile = new File(outputDir, filePath);

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error saving converted image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "External storage is not available or writable", Toast.LENGTH_SHORT).show();
        }

















//        File outputDir = getExternalCacheDir();
        if (outputDir != null) {
            File outputFile;
            String extension;

            if (radioJpg.isChecked()) {
                outputFile = new File(outputDir, "converted_image.jpg");
                extension = "JPG";
            } else if (radioJpeg.isChecked()) {
                outputFile = new File(outputDir, "converted_image.jpeg");
                extension = "JPEG";

            } else if (radiopng.isChecked()) {
                    outputFile = new File(outputDir, "converted_image.png");
                    extension = "PNG";
            } else if (radioPdf.isChecked()) {
                outputFile = new File(outputDir, "converted_image.pdf");
                extension = "PDF";
            } else {
                Toast.makeText(getApplicationContext(), "Please select an output format.", Toast.LENGTH_SHORT).show();
                return;
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                if (extension.equals("JPG")) {
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } else if (extension.equals("JPEG")) {
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } else if (extension.equals("PDF")) {
                    convertToPdf(selectedImage, out);
                }
                else if (extension.equals("PNG")) {
                    selectedImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                    convertedFile = outputFile;
                btnDownload.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Image converted and saved as " + extension, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error saving converted image.", Toast.LENGTH_SHORT).show();
            }
        }

        final int maxProgress = 100; // Maximum progress value
        final ProgressBar progressBar = findViewById(R.id.uploadProgressBar);

        AsyncTask<Void, Integer, Boolean> uploadTask = new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                // Simulate the upload progress (replace with your actual upload logic)
                int totalProgress = 0;
                int step = 10; // Adjust the step size as needed
                while (totalProgress <= maxProgress) {
                    try {
                        Thread.sleep(1000); // Simulate a delay
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    totalProgress += step;
                    publishProgress(totalProgress);
                }
                return true; // Return true when upload is complete
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                progressBar.setProgress(values[0]);
                uploadPercentageTextView.setText(values[0] + "%"); // Update the percentage TextView
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    // Upload is complete
                    progressBar.setVisibility(View.INVISIBLE);
                    uploadPercentageTextView.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "File uploaded", Toast.LENGTH_SHORT).show();
                }
            }
        };

        progressBar.setVisibility(View.VISIBLE);
        uploadPercentageTextView.setVisibility(View.VISIBLE);

        // Execute the upload task
        uploadTask.execute();


    }

    private String constructFilePath() {
        // Custom file name handling
        String fileName = fileNameEditText.getText().toString().trim();
        String selectedExtension = "";

        if (fileName.isEmpty()) {
            // If no custom name provided, generate a unique file name
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            fileName = "image_" + timestamp;
//            String selectedExtension = "";


            // Determine the selected extension based on the radio button
            int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            if (selectedRadioButtonId == R.id.radioJpg) {
                selectedExtension = "jpg";
            } else if (selectedRadioButtonId == R.id.radioJpeg) {
                selectedExtension = "jpeg";
            } else if (selectedRadioButtonId == R.id.radiopng) {
                selectedExtension = "png";
            } else if (selectedRadioButtonId == R.id.radioPdf) {
                selectedExtension = "pdf";
            }
        } else {
            // Determine the selected extension based on the radio button (fallback to PDF if not selected)
            int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            if (selectedRadioButtonId == R.id.radioJpg) {
                selectedExtension = "jpg";
            } else if (selectedRadioButtonId == R.id.radioJpeg) {
                selectedExtension = "jpeg";
            } else if (selectedRadioButtonId == R.id.radiopng) {
                selectedExtension = "png";
            } else {
                // If no radio button selected, default to PDF
                selectedExtension = "pdf";
            }
        }

        // Construct the file path with the selected extension
        return fileName + "." + selectedExtension;
    }


    private void convertToPdf(Bitmap image, FileOutputStream out) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(image.getWidth(), image.getHeight(), 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        page.getCanvas().drawBitmap(image, 0, 0, null);
        pdfDocument.finishPage(page);

        try {
            pdfDocument.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        pdfDocument.close();
    }


    private String getMimeType(String fileName) {
        String mimeType = "application/octet-stream"; // Default MIME type

        if (fileName != null) {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.US);
                switch (extension) {
                    case "jpg":
                    case "jpeg":
                        mimeType = "image/jpeg";
                        break;
                    case "pdf":
                        mimeType = "application/pdf";
                        break;
                    case "png":
                        mimeType = "application/png";
                        break;

                    // Add more cases for other file types if needed
                }
            }
        }

        return mimeType;
    }
}






