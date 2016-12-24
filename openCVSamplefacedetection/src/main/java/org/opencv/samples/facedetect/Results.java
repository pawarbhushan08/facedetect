package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Results extends Activity {
    private EditText patientName;
    private EditText dob;
    private Button btnSave;
    private RadioGroup gndr;
    private RadioButton selectedBtn;
    private EditText examiner;
    private TextView typeNyst;
    Image image = null;

    private static String FILE = "/sdcard/ReportFile.pdf";
    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18,
            Font.BOLD);
    private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
            Font.NORMAL, BaseColor.RED);
    private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16,
            Font.BOLD);
    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12,
            Font.BOLD);
    String datatoCollect="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        patientName = (EditText)findViewById(R.id.patientName);
        dob = (EditText)findViewById(R.id.DOB);

        gndr = (RadioGroup) findViewById(R.id.gender);
        int selectedId = gndr.getCheckedRadioButtonId();
        Log.d("ID",""+selectedId);
        selectedBtn = (RadioButton) findViewById(selectedId);

        examiner = (EditText)findViewById(R.id.examiner);


        Intent intent = getIntent();
        datatoCollect = intent.getStringExtra("Nystagmus_Type");
        Log.v("eyeSelect0",datatoCollect);

        typeNyst = (TextView)findViewById(R.id.TON);
        typeNyst.setText(datatoCollect);
        addListenerOnButton();
    }

    public void addListenerOnButton() {

        final Context context = this;

        btnSave = (Button) findViewById(R.id.save);

        btnSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {


                try {
                    Document document = new Document();
                    PdfWriter.getInstance(document, new FileOutputStream(FILE));
                    document.open();
                    //addMetaData(document);
                    addTitlePage(document);
                    document.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }



        }

        });

        Button Exit = (Button) findViewById(R.id.exit);
        Exit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(getApplicationContext(), AndroidVideoCaptureExample.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);

                System.exit(0);
            }
        });

    }

      // iText allows to add metadata to the PDF which can be viewed in your Adobe
        // Reader
        // under File -> Properties
        /*private void addMetaData(Document document) {
            document.addTitle("My first PDF");
            document.addSubject("Using iText");
            document.addKeywords("Java, PDF, iText");
            document.addAuthor("Lars Vogel");
            document.addCreator("Lars Vogel");
        }*/

        private void addTitlePage(Document document)
                throws DocumentException, IOException {
            Paragraph preface = new Paragraph();
            // We add one empty line
            addEmptyLine(preface, 1);
            // Lets write a big header
            // Will create: Report generated by: _name, _date
            preface.add(new Paragraph(
                    "Report generated by: " + "Nystagmus detector app" + ", " + new Date(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    smallBold));

            addEmptyLine(preface, 1);

            preface.add(new Paragraph("Patient Name: "+patientName.getText(), catFont));

            addEmptyLine(preface, 1);

            preface.add(new Paragraph("Date of Birth: "+dob.getText(), catFont));

            addEmptyLine(preface, 1);

            preface.add(new Paragraph("Gender: "+selectedBtn.getText(), catFont));

            addEmptyLine(preface, 1);

            preface.add(new Paragraph("Examiner: "+examiner.getText(), catFont));

            addEmptyLine(preface, 1);

            preface.add(new Paragraph("Nystagmus Detected: "+typeNyst.getText(), catFont));

            addEmptyLine(preface, 1);
            preface.add(new Paragraph("Result: "+"", catFont));
            addEmptyLine(preface, 1);

            image = Image.getInstance("/sdcard/DCIM/chart.jpg");
            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                    - document.rightMargin()) / image.getWidth()) * 100;

            image.scalePercent(scaler);
            Log.v("Image Loaded",""+image.type());


                //image.scaleAbsolute(150f, 150f);
                //image.scalePercent(-50f);
            preface.add(image);

            addEmptyLine(preface,4);

            preface.add(new Paragraph("Signature of Examiner: "+"", catFont));

            document.add(preface);
            // Start a new page
            document.newPage();
        }

       /* private void addContent(Document document) throws DocumentException {
            Anchor anchor = new Anchor("First Chapter", catFont);
            anchor.setName("First Chapter");

            // Second parameter is the number of the chapter
            Chapter catPart = new Chapter(new Paragraph(anchor), 1);

            Paragraph subPara = new Paragraph("Subcategory 1", subFont);
            Section subCatPart = catPart.addSection(subPara);
            subCatPart.add(new Paragraph("Hello"));

            subPara = new Paragraph("Subcategory 2", subFont);
            subCatPart = catPart.addSection(subPara);
            subCatPart.add(new Paragraph("Paragraph 1"));
            subCatPart.add(new Paragraph("Paragraph 2"));
            subCatPart.add(new Paragraph("Paragraph 3"));

            // add a list
            createList(subCatPart);
            Paragraph paragraph = new Paragraph();
            addEmptyLine(paragraph, 5);
            subCatPart.add(paragraph);

            // add a table
            createTable(subCatPart);

            // now add all this to the document
            document.add(catPart);

            // Next section
            anchor = new Anchor("Second Chapter", catFont);
            anchor.setName("Second Chapter");

            // Second parameter is the number of the chapter
            catPart = new Chapter(new Paragraph(anchor), 1);

            subPara = new Paragraph("Subcategory", subFont);
            subCatPart = catPart.addSection(subPara);
            subCatPart.add(new Paragraph("This is a very important message"));

            // now add all this to the document
            document.add(catPart);

        }*/

        /*private void createTable(Section subCatPart)
                throws BadElementException {
            PdfPTable table = new PdfPTable(3);

            // t.setBorderColor(BaseColor.GRAY);
            // t.setPadding(4);
            // t.setSpacing(4);
            // t.setBorderWidth(1);

            PdfPCell c1 = new PdfPCell(new Phrase("Table Header 1"));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            c1 = new PdfPCell(new Phrase("Table Header 2"));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            c1 = new PdfPCell(new Phrase("Table Header 3"));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);
            table.setHeaderRows(1);

            table.addCell("1.0");
            table.addCell("1.1");
            table.addCell("1.2");
            table.addCell("2.1");
            table.addCell("2.2");
            table.addCell("2.3");

            subCatPart.add(table);

        }

        private void createList(Section subCatPart) {
            List list = new List(true, false, 10);
            list.add(new ListItem("First point"));
            list.add(new ListItem("Second point"));
            list.add(new ListItem("Third point"));
            subCatPart.add(list);
        }*/

        private void addEmptyLine(Paragraph paragraph, int number) {
            for (int i = 0; i < number; i++) {
                paragraph.add(new Paragraph(" "));
            }
        }
    }

