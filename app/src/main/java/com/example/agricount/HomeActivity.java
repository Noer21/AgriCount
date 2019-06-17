package com.example.agricount;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.epson.lwprint.sdk.LWPrint;
import com.epson.lwprint.sdk.LWPrintCallback;
import com.epson.lwprint.sdk.LWPrintDataProvider;
import com.epson.lwprint.sdk.LWPrintDiscoverPrinter;
import com.epson.lwprint.sdk.LWPrintDiscoverPrinterCallback;
import com.epson.lwprint.sdk.LWPrintParameterKey;
import com.epson.lwprint.sdk.LWPrintPrintingPhase;
import com.epson.lwprint.sdk.LWPrintTapeCut;
import com.epson.lwprint.sdk.LWPrintTapeOperation;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private RequestQueue mQueue;
    private ProgressDialog progressBar;
    private String url = "http://agricount.codepanda.id/parameters/cetak-bar-code";
    private Button printCodeButton;

    // Printer Var
    private Map<String, Integer> printerStatus;
    private Integer tapeWidth;

    // Bitmap
    private Bitmap bmp;

    // Session
    private SharedPreferences sharedPreferences;

    // Printer
    LWPrint lwPrint;

    // DataProvider
    PrintDataProvider printDataProvider;

    //Barcode Info
    String valCompanyName = "Perusahaan Name",
            valCompanyAddress = "Perusahaan Address",
            valFruitVariety = "Mangga Asam manis",
            valPestiside = "Anti Serangga",
            valFertilizer = "Pupuk Kandang",
            fullData;
    String valFruitNo = "25";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPreferences = getSharedPreferences("mysharedpreference", MODE_PRIVATE);

        printDataProvider = new PrintDataProvider();
        mQueue = Volley.newRequestQueue(this);

        findPrinter();
        firstEncode();

        printCodeButton = findViewById(R.id.printcode);
        printCodeButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNewCode();
            }
        });

        // creating progress bar dialog
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Mencari Printer, pastikan sudah terhubung dengan Bluetooth anda ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();
    }

    private void firstEncode(){
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Date Harvest
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMMM yyyy");
            String date = new Date().toString();
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(fmt.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            c.add(Calendar.DATE, 40);
            date = fmt.format(c.getTime());

            Integer.parseInt(valFruitNo);
            valFruitNo = valFruitNo + 1;
            String Data = "Nama Perusahaan    : " + valCompanyName
                    + "\nAlamat   : " + valCompanyAddress
                    + "\nVarietas Buah    : " + valFruitVariety
                    + "\nTanggal Panen    : " + date
                    + "\nBuah ke-" + valFruitNo
                    //+ "\nJenis Pupuk  : " + valFertilizer
                    + "\nJenis Pestisida  : " + valPestiside;
            BitMatrix bitMatrix = writer.encode(Data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            ((ImageView) findViewById(R.id.img_result_qr)).setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void generateNewCode(){
        // creating progress bar dialog
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Mengunggah data ke server ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();

        // Take Data From Server
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject data = response.getJSONObject("data");
                            Log.d("kribo", data.toString());
                            // 1
                            JSONObject companyName = data.getJSONObject("companyName");
                            valCompanyName = companyName.getString("value");
                            // 2
                            JSONObject companyAddress = data.getJSONObject("companyAddress");
                            valCompanyAddress = companyAddress.getString("value");
                            // 3
                            JSONObject Fertilizer = data.getJSONObject("jenisPupuk");
                            valFertilizer = Fertilizer.getString("value");
                            // 4
                            JSONObject Pestiside = data.getJSONObject("jenisPestisida");
                            valPestiside = Pestiside.getString("value");
                            // 5
                            JSONObject fruitNo = data.getJSONObject("buahKe");
                            valFruitNo = fruitNo.getString("value");
                            // 6
                            JSONObject FruitVariety = data.getJSONObject("varietasBuah");
                            valFruitVariety = FruitVariety.getString("value");
                            progressBar.dismiss();
                            encode();


                        } catch (JSONException e){
                            e.printStackTrace();
                            progressBar.dismiss();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("kriboerror", error.toString());
            }
        }
        );
        mQueue.add(request);


    }

    public void encode(){
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Membuat QR Code ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();

        //Encode Data
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Date Harvest
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMMM yyyy");
            String date = new Date().toString();
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(fmt.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            c.add(Calendar.DATE, 40);
            date = fmt.format(c.getTime());

            Integer.parseInt(valFruitNo);
            String Data = "Nama Perusahaan    : " + valCompanyName
                    + "\nAlamat   : " + valCompanyAddress
                    + "\nVarietas Buah    : " + valFruitVariety
                    + "\nTanggal Panen    : " + date
                    + "\nBuah ke-" + valFruitNo.toString()
                    //+ "\nJenis Pupuk  : " + valFertilizer
                    + "\nJenis Pestisida  : " + valPestiside;

            Log.d("Printing_data", Data);
            printDataProvider.setQrCodeData(Data);
            BitMatrix bitMatrix = writer.encode(Data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            ((ImageView) findViewById(R.id.img_result_qr)).setImageBitmap(bmp);
            progressBar.dismiss();

            // print
            printCode();
        } catch (WriterException e) {
            e.printStackTrace();
            progressBar.dismiss();
        }
    }

    public void printCode(){
        //Loading
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Printing ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();

        // printerParameterKeys
        Map<String, Object> printerParameterKeys = new HashMap<String, Object>() {{
            put("Copies", 1);
            put("TapeCut", LWPrintTapeCut.EachLabel);
            put("HalfCut", true);
            put("PrintSpeed", false);
            put("Density", 0);
            put("TapeWidth", tapeWidth);
        }};

        // set LWPrinterCallback
        LWPrintCallback lwPrintCallback = new LWPrintCallback() {
            @Override
            public void onChangePrintOperationPhase(LWPrint lwPrint, int i) {
                int temp = i;
                String phase = String.valueOf(temp);
                if (temp == 4){
                    progressBar.dismiss();
                }
                Log.d("printing_phase_printn", phase);
            }

            @Override
            public void onSuspendPrintOperation(LWPrint lwPrint, int i, int i1) {
                Log.d("printing_suspended", "error");
            }

            @Override
            public void onAbortPrintOperation(LWPrint lwPrint, int i, int i1) {
                Log.d("printing_abort", "error");
            }

            @Override
            public void onChangeTapeFeedOperationPhase(LWPrint lwPrint, int i) {
                int temp = i;
                String phase = String.valueOf(temp);
                Log.d("printing_phase_tape", phase);
            }

            @Override
            public void onAbortTapeFeedOperation(LWPrint lwPrint, int i, int i1) {
                Log.d("printing_changeabort", "error");
            }
        };
        lwPrint.setCallback(lwPrintCallback);

        // Start Printing
        Log.d("Printing_param", printDataProvider.getQrCodeData().toString());
        Log.d("printing_param1", printDataProvider.toString());
        Log.d("printing_param2", printerParameterKeys.toString());
        Log.d("printing_bitmap", bmp.toString());
        lwPrint.doPrint(printDataProvider, printerParameterKeys);
    }

    private void findPrinter(){

        // LWPrinter
        lwPrint = new LWPrint(this);

        // LWPrintDiscoverPrinterCallback
        LWPrintDiscoverPrinterCallback lwPrintDiscoverPrinterCallback = new LWPrintDiscoverPrinterCallback() {
            @Override
            public void onFindPrinter(LWPrintDiscoverPrinter lwPrintDiscoverPrinter, Map<String, String> map) {
                lwPrint.setPrinterInformation(map);
                printerStatus = lwPrint.fetchPrinterStatus();
                tapeWidth = lwPrint.getTapeWidthFromStatus(printerStatus);
                Log.d("loadPage_printer", "Found One!");
                Log.d("loadPage_printerStatus", printerStatus.toString());
                Log.d("loadPage_tapeWidth", tapeWidth.toString());
                lwPrintDiscoverPrinter.stopDiscover();
                progressBar.dismiss();
                Toast.makeText(HomeActivity.this, "Printer ditemukan!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemovePrinter(LWPrintDiscoverPrinter lwPrintDiscoverPrinter, Map<String, String> map) {
                Log.d("printer", "Lost One!");
                progressBar.dismiss();
            }
        };

        // LWPrintDiscoverPrinter
        LWPrintDiscoverPrinter lwPrintDiscoverPrinter = new LWPrintDiscoverPrinter();
        lwPrintDiscoverPrinter.setCallback(lwPrintDiscoverPrinterCallback);
        lwPrintDiscoverPrinter.startDiscover(this);
    }

    class PrintDataProvider implements LWPrintDataProvider{

        private static final String FORM_DATA_QRCODE = "FormDataQRCode.plist";
        private String qrCodeData = "QRCode";
        InputStream formDataQRCodeInputStream;

        public void closeStreams() {
            if (formDataQRCodeInputStream != null) {
                try {
                    formDataQRCodeInputStream.close();
                } catch (IOException e) {
                    Log.d("Stream", "Close Stream error");
                }
                formDataQRCodeInputStream = null;
            }
        }

        public String getQrCodeData() {
            return qrCodeData;
        }

        public void setQrCodeData(String qrCodeData) {
            this.qrCodeData = qrCodeData;
        }

        @Override
        public void startOfPrint() {
            // It is called only once when printing started
            Log.d("Data Provider","startOfPrint");
        }

        @Override
        public void endOfPrint() {
            // It is called only once when printing finished
            Log.d("Data Provider","endOfPrint");
        }

        @Override
        public void startPage() {
            // It is called when starting a page
            Log.d("Data Provider","startPage");
        }

        @Override
        public void endPage() {
            // It is called when finishing a page
            Log.d("Data Provider","endOfPrint");
        }

        @Override
        public int getNumberOfPages() {
            // Return all pages printed
            Log.d("Data Provider","getNumberOfPages");

            return 1;
        }

        public InputStream getFormDataForPage(int pageIndex) {
            // Return the form data for pageIndex page
            Log.d("Data Provider","getFormDataForPage: pageIndex=" + pageIndex);

            InputStream formData = null;

            Log.d("Data Provider","QRCode: pageIndex=" + pageIndex);
            if (formDataQRCodeInputStream != null) {
                try {
                    formDataQRCodeInputStream.close();
                } catch (IOException e) {
                    Log.e("Data Provider", e.toString(), e);
                }
                formDataQRCodeInputStream = null;
            }
            try {
                AssetManager as = getResources().getAssets();
                formDataQRCodeInputStream = as.open(FORM_DATA_QRCODE);
                formData = formDataQRCodeInputStream;
                Log.d("Data Provider","getFormDataForPage: " + FORM_DATA_QRCODE + "=" + formDataQRCodeInputStream.available());
            } catch (IOException e) {
                Log.e("Data Provider", e.toString(), e);
            }

            return formData;
        }

        @Override
        public String getStringContentData(String s, int i) {
            return qrCodeData;
        }

        @Override
        public Bitmap getBitmapContentData(String s, int i) {
            return null;
        }

    }



}
