package com.example.mate.gooday_mate;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mate.gooday_mate.Fragment.PatientDialogFragment;
import com.example.mate.gooday_mate.adapter.MainAdapter;
import com.example.mate.gooday_mate.service.Config;
import com.example.mate.gooday_mate.service.Item_Main;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MainAdapter.ItemListener, PatientDialogFragment.PatientDialogFragmentListener {
    String SHOWDATA_URL = Config.URL + "show_patient.php";
    String CHECKDATA_URL = Config.URL + "patient_register.php";
    MainAdapter mainAdapter;
    private String patient_JSON = null;
    private LinearLayoutManager layoutManager;
    private RecyclerView recyclerView;
    private AlertDialog alertDialog;

    //qr code scanner object
    private IntentIntegrator qrScan;
    private JSONObject jsonObj;

    JSONArray contents = null;
    ArrayList<Item_Main> items = new ArrayList<>();
    String myJSON;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("LOG_onCreate", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().subscribeToTopic("news");
        FirebaseInstanceId.getInstance().getToken();

        initViews();
    }

    private void initViews() {
        Log.i("LOG_1ininViews", "beforegetdata");
        getData(SHOWDATA_URL);

        layoutManager = new GridLayoutManager(MainActivity.this, 3);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);

        //intializing scan object
        qrScan = new IntentIntegrator(this);

        findViewById(R.id.manager).setOnClickListener(this);
        findViewById(R.id.search_patient).setOnClickListener(this);
        findViewById(R.id.ic_qr).setOnClickListener(this);
        findViewById(R.id.ic_search).setOnClickListener(this);
        findViewById(R.id.ic_notice).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.manager:
                AlertDialog.Builder managerbuilder = new AlertDialog.Builder(this);
                managerbuilder.setTitle("Smart Life Care 과정 1기 연수생");
                managerbuilder.setMessage("    개발자      강은진 (kof2289@gmail.com)  \n    개발자      권미나 (kmeena0924@gmail.com)     ");
                alertDialog = managerbuilder.create();
                alertDialog.show();
                break;
            case R.id.search_patient:
                LayoutInflater inflater = getLayoutInflater();

                final View dialogView = inflater.inflate(R.layout.dialog2, null);
                AlertDialog.Builder addbuilder = new AlertDialog.Builder(this);
                addbuilder.setView(dialogView);
                addbuilder.setNegativeButton("취소", null);
                addbuilder.setPositiveButton("검색", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        EditText edit_name = dialogView.findViewById(R.id.name);
                        EditText edit_birth = dialogView.findViewById(R.id.birth);

                        HashMap<String, String> patientMap = new HashMap<>();
                        patientMap.put("name", edit_name.getText().toString());
                        patientMap.put("birth", edit_birth.getText().toString());
                        Intent intent = new Intent(MainActivity.this, PatientInfoDialogActivity.class);
                        intent.putExtra("patientMap", patientMap);
                        startActivity(intent);
                    }
                });
                alertDialog = addbuilder.create();
                alertDialog.show();
                break;
            case R.id.ic_qr:
                qrScan.setPrompt("Scanning...");
                qrScan.initiateScan();
                break;
            case R.id.ic_search:
                //       startActivity(new Intent(MainActivity.this, MapActivity.class));
                break;
            case R.id.ic_notice:
                startActivity(new Intent(MainActivity.this, NoticeActivity.class));
                break;
        }
    }

    @Override
    public void onItemClick(final Item_Main item) {
        AlertDialog.Builder showbuilder = new AlertDialog.Builder(this);
        showbuilder.setTitle("my Patient");
        showbuilder.setMessage(item.getName() + " 님" + "\n" + "생년월일    " + item.getBirth() + "  " + "입원일   " + item.getEnterdate());
        Config.PATIENT_NAME = item.getName();
        showbuilder.setNeutralButton("대화하기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Toast.makeText(getApplicationContext(), "개발중입니다", Toast.LENGTH_SHORT).show();
            }
        });
        showbuilder.setNegativeButton("보호자와 통화", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.getPhone())));
            }
        });

        showbuilder.setPositiveButton("현재상태 확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                String json = "{'name':'" + item.getName() + "','birth':'" + item.getBirth() + "','sex':'" + item.getSex() + "','phone':'" + item.getPhone() + "','enterdate':'" + item.getEnterdate() + "','image':'" + item.getImg() + "','channel':'" + item.getChannel() + "','port':'" + item.getPort() + "'}";
                insertToDatabase(CHECKDATA_URL, json);
            }
        });
        alertDialog = showbuilder.create();
        alertDialog.show();
    }


    @Override
    public void onPatientDialogClick(DialogFragment dialogFragment, String someData) {
    }

    private void getData(String data_url) {
        class GetDataJSON extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                String uri = params[0];
                StringBuilder sb = new StringBuilder();
                BufferedReader bufferedReader = null;

                try {
                    URL url = new URL(uri);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                    String json;
                    while ((json = bufferedReader.readLine()) != null) {
                        sb.append(json + "\n");
                    }
                    return sb.toString();

                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                myJSON = result;
                showList();
            }

        }
        GetDataJSON g = new GetDataJSON();
        g.execute(data_url);
    }

    private void insertToDatabase(String url, String patient_JSON) {
        Log.i("LOG_8stinsert", url);

        class insertData extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                Log.i("LOG_11doI", params[0]);
                String uri = params[0];
                StringBuilder sb = new StringBuilder();
                BufferedReader bufferedReader = null;

                try {
                    URL url = new URL(uri);
                    Log.i("LOG_12doI", params[1]);
                    jsonObj = new JSONObject(params[1]);
                    String data = URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("name"), "UTF-8");
                    data += "&" + URLEncoder.encode("birth", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("birth"), "UTF-8");
                    data += "&" + URLEncoder.encode("sex", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("sex"), "UTF-8");
                    data += "&" + URLEncoder.encode("phone", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("phone"), "UTF-8");
                    data += "&" + URLEncoder.encode("enterdate", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("enterdate"), "UTF-8");
                    data += "&" + URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("image"), "UTF-8");
                    data += "&" + URLEncoder.encode("channel", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("channel"), "UTF-8");
                    data += "&" + URLEncoder.encode("port", "UTF-8") + "=" + URLEncoder.encode(jsonObj.getString("port"), "UTF-8");

                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    Log.i("LOG_13_1sb", data);

                    wr.write(data);
                    Log.i("LOG_13_2sb", wr.toString());
                    wr.flush();
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    Log.i("LOG_13_3sb", bufferedReader.readLine());

                    String line = null;
                    // Read Server Response
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    Log.i("LOG_13_4sb", sb.toString());
                    return sb.toString();

                } catch (Exception e) {
                    Log.i("LOG_Exception", e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Log.i("LOG_14Post", result);

                if (result.contains("already")) {
                    Intent showIntent = new Intent(MainActivity.this, ShowPatientActivity.class);
                    String json = result.substring(14);
                    showIntent.putExtra("patientJSON", json);
                    startActivity(showIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "추가완료", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    finish();
                }
            }
        }
        insertData g = new insertData();
        g.execute(url, patient_JSON);
    }

    protected void showList() {
        try {
            jsonObj = new JSONObject(myJSON);
            contents = jsonObj.getJSONArray("result");

            for (int i = 0; i < contents.length(); i++) {
                JSONObject c = contents.getJSONObject(i);
                String id = c.getString("id");
                String name = c.getString("name");
                String birth = c.getString("birth");
                String sex = c.getString("sex");
                String image = c.getString("image");
                String phone = c.getString("phone");
                String enterdate = c.getString("enterdate");
                String channel = c.getString("channel");
                String port = c.getString("port");

                items.add(new Item_Main(name, birth, sex, enterdate, phone, getId(image), channel, port));
            }
            mainAdapter = new MainAdapter(this, items, this);
            recyclerView.setAdapter(mainAdapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int getId(String name) {
        int tempId = getResources().getIdentifier(name, "mipmap", this.getPackageName());
        return tempId;
    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) { //qrcode 가 없으면
            if (result.getContents() == null) {
                Toast.makeText(MainActivity.this, "취소!", Toast.LENGTH_SHORT).show();
            } else { //qrcode 결과가 있으면
                patient_JSON = result.getContents();
                insertToDatabase(CHECKDATA_URL, patient_JSON);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}