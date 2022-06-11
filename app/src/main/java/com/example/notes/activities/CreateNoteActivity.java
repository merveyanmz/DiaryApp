package com.example.notes.activities;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.notes.R;
import com.example.notes.database.NoteDatabase;
import com.example.notes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;
    private View viewSubtitleIndicator;
    private ImageView imageNote;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;

    private String selectedNoteColor;
    private String selectedImagePath;

    private static final int REQUEST_CODE_STORAGE_PERMISSION=1;
    private static final int REQUEST_CODE_SELECT_IMAGE=2;

    private static final int PERMISSION_REQUEST_CODE = 200;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;
    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        ImageView imageBack=findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        inputNoteTitle= findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle=findViewById(R.id.inputNoteSubtitle);
        inputNoteText=findViewById(R.id.inputNotes);
        textDateTime=findViewById(R.id.textDateTime);
        viewSubtitleIndicator=findViewById(R.id.viewSubtitleIndicator);
        imageNote=findViewById(R.id.imageNote);
        textWebURL=findViewById(R.id.textWebURL);
        layoutWebURL=findViewById(R.id.layoutWebURL);

        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                .format(new Date())
        );

        ImageView imageSave=findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        selectedNoteColor="#333333";
        selectedImagePath="";

        if (getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote= (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveWebURL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath="";
            }
        });

        if (getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type=getIntent().getStringExtra("quickActionType");
            if (type!=null){
                if (type.equals("image")){
                    selectedImagePath=getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }
            }
        }

        initMiscellaneous();
        setSubtitleIndicatorColor();


        if (checkPermission()!=true) {
            requestPermission();
        }


    }

    private void setViewOrUpdateNote(){
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getGubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() !=null && !alreadyAvailableNote.getImagePath().trim().isEmpty()){
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath=alreadyAvailableNote.getImagePath();
        }
        if (alreadyAvailableNote.getWebLink()!=null && !alreadyAvailableNote.getWebLink().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }


    private void saveNote(){
        if (inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        else if(inputNoteSubtitle.getText().toString().trim().isEmpty()
                && inputNoteText.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        final Note note=new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setGubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if (layoutWebURL.getVisibility()==View.VISIBLE){
            note.setWebLink(textWebURL.getText().toString());
        }

        if (alreadyAvailableNote!=null){
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void,Void>{

            @Override
            protected Void doInBackground(Void... voids) {
                NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertNode(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Intent intent=new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
        Toast.makeText(this, "New note is created successfully!", Toast.LENGTH_SHORT).show();

    }

   private void initMiscellaneous(){
        final LinearLayout layoutMiscellaneous =findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior=BottomSheetBehavior.from(layoutMiscellaneous);
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() !=BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else{
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1=layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2=layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3=layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4=layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5=layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedNoteColor="#333333";
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

       layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               selectedNoteColor="#FDBE3B";
               imageColor1.setImageResource(0);
               imageColor2.setImageResource(R.drawable.ic_done);
               imageColor3.setImageResource(0);
               imageColor4.setImageResource(0);
               imageColor5.setImageResource(0);
               setSubtitleIndicatorColor();
           }
       });

       layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               selectedNoteColor="#FF4842";
               imageColor1.setImageResource(0);
               imageColor2.setImageResource(0);
               imageColor3.setImageResource(R.drawable.ic_done);
               imageColor4.setImageResource(0);
               imageColor5.setImageResource(0);
               setSubtitleIndicatorColor();
           }
       });

       layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               selectedNoteColor="#3A52Fc";
               imageColor1.setImageResource(0);
               imageColor2.setImageResource(0);
               imageColor3.setImageResource(0);
               imageColor4.setImageResource(R.drawable.ic_done);
               imageColor5.setImageResource(0);
               setSubtitleIndicatorColor();
           }
       });

       layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               selectedNoteColor="#000000";
               imageColor1.setImageResource(0);
               imageColor2.setImageResource(0);
               imageColor3.setImageResource(0);
               imageColor4.setImageResource(0);
               imageColor5.setImageResource(R.drawable.ic_done);
               setSubtitleIndicatorColor();
           }
       });


       if (alreadyAvailableNote!=null &&alreadyAvailableNote.getColor()!=null&& !alreadyAvailableNote.getColor().trim().isEmpty()){
           switch (alreadyAvailableNote.getColor()){
               case "#FDBE3B":
                   layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
               case "#FF4842":
                   layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
               case "#3A52Fc":
                   layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
               case "#000000":
                   layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
           }
       }

       layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
               if (ContextCompat.checkSelfPermission(
                       getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
               ) != PackageManager.PERMISSION_GRANTED){
                   ActivityCompat.requestPermissions(
                           CreateNoteActivity.this,
                           new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                           REQUEST_CODE_STORAGE_PERMISSION
                   );
               } else{
                   selectImage();
               }
           }
       });

       layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddURLDialog();
           }
       });


       layoutMiscellaneous.findViewById(R.id.layoutPDF).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               generatePDF();
           }
       });


       layoutMiscellaneous.findViewById(R.id.layoutSMS).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               sendNote();
           }
       });



       if (alreadyAvailableNote!=null){
           layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
           layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
               }
           });
       }
   }

   private void showDeleteNoteDialog(){
        if (dialogDeleteNote==null){
            AlertDialog.Builder builder=new AlertDialog.Builder(CreateNoteActivity.this);
            View view=LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);
            dialogDeleteNote=builder.create();
            if (dialogDeleteNote.getWindow()!=null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().deleteNode(alreadyAvailableNote);

                            return null;
                        }
                        @Override
                        protected void onPostExecute(Void aVoid){
                            super.onPostExecute(aVoid);
                            Intent intent=new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        }

                    }
                    new DeleteNoteTask().execute();
                    Toast.makeText(CreateNoteActivity.this, "The note is deleted successfully!", Toast.LENGTH_SHORT).show();
                }
            });
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        dialogDeleteNote.show();
   }


   private void setSubtitleIndicatorColor(){
       GradientDrawable gradientDrawable=(GradientDrawable) viewSubtitleIndicator.getBackground();
       gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
   }

   private void selectImage(){
        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }

   }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                selectImage();
            }
            else{
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_CODE_SELECT_IMAGE &&resultCode ==RESULT_OK){
            if (data!=null){
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null){
                    try {
                        InputStream inputStream=getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectedImagePath=getPathFromUri(selectedImageUri);
                    }catch (Exception exception){
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor=getContentResolver()
                .query(contentUri,null,null,null,null);
        if (cursor==null){
            filePath=contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int index=cursor.getColumnIndex("_data");
            filePath=cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void showAddURLDialog(){
        if (dialogAddURL==null){
            AlertDialog.Builder builder= new AlertDialog.Builder(CreateNoteActivity.this);
            View view= LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddURL=builder.create();
            if (dialogAddURL.getWindow()!=null){
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL=view.findViewById(R.id.inputURL);
            inputURL.requestFocus();
            
            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (inputURL.getText().toString().trim().isEmpty()){
                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
                        Toast.makeText(CreateNoteActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                    } else{
                        textWebURL.setText(inputURL.getText().toString());
                        layoutWebURL.setVisibility(View.VISIBLE);
                        dialogAddURL.dismiss();
                    }
                }
            });


            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();

    }


    public void generatePDF(){

        PdfDocument pdfDocument = new PdfDocument();

        Paint paint = new Paint();
        Paint title = new Paint();

        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(250, 400, 1).create();

        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);

        Canvas canvas = myPage.getCanvas();

       // canvas.drawBitmap(scaledbmp, 56, 40, paint);

        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextSize(15);
        title.setColor(ContextCompat.getColor(this, R.color.colorDefaultNoteColor));


        String noteT=alreadyAvailableNote.getTitle();
        String noteS=alreadyAvailableNote.getGubtitle();
        String noteTe=alreadyAvailableNote.getNoteText();

        canvas.drawText(noteT, 25, 25, title);
        canvas.drawText(noteS, 25, 45, title);
        canvas.drawText(noteTe, 25, 65, title);

        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        title.setColor(ContextCompat.getColor(this, R.color.colorDefaultNoteColor));
        title.setTextSize(15);

        title.setTextAlign(Paint.Align.CENTER);
       // canvas.drawText("This is sample document which we have created.", 396, 560, title);

        pdfDocument.finishPage(myPage);

        // below line is used to set the name of
        // our PDF file and its path.
        File file = new File(Environment.getExternalStorageDirectory(), "MyNote.pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(CreateNoteActivity.this, "PDF file generated successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();

    }


    protected void sendNote() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        try {
            startActivity(sendIntent);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private boolean checkPermission() {
        // checking of permissions.
        int permission1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int permission2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        // requesting permissions if not provided.
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }


}